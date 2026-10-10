package com.example.demo.service;

import com.example.demo.dto.AccountForm;
import com.example.demo.dto.AccountSearchForm;
import com.example.demo.entity.Account;
import com.example.demo.entity.AccountDetail;
import com.example.demo.entity.AccountCategory;
import com.example.demo.mapper.AccountMapper;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.AcCategoryRepository;
import com.example.demo.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AcCategoryRepository categoryRepository;

    @Autowired
    private AccountMapper accountMapper; // MapStructの自動生成クラスをDI

    @Autowired
    private FileStorageService fileStorageService; // 設定ファイルに応じて、LocalかAzureが自動で入る

    @Transactional
    public void saveAccount(AccountForm form, String userName) {
        // 【追加】明細リストから、商品名も金額も空の要素を除外する
        if (form.getDetails() != null) {
            form.getDetails().removeIf(detail ->
                (detail.getItemName() == null || detail.getItemName().trim().isEmpty()) &&
                detail.getItemAmount() == null
            );
        }

        // 更新の場合は、フォームのaccountIdが「ログイン中ユーザー本人のデータ」であることを確認する。
        // （確認しないと、他人のIDを送るだけでそのユーザーの家計簿を上書きできてしまう）
        Account existing = null;
        if (form.getAccountId() != null) {
            existing = findByIdAndOwner(form.getAccountId(), userName);
        }

        // 画像パスは画面の隠し項目から届くため信用せず、許可できる値かをここで検証する。
        // 一時フォルダの画像は、ここで本保存先（NAS / S3 など）へ転送される。
        String requestedImagePath = form.getReceiptImagePath();
        String finalImagePath = resolveImagePath(requestedImagePath, existing, userName);
        form.setReceiptImagePath(finalImagePath);
        // 転送した場合だけ、本保存先のパスが元のパス（/images/temp/...）と変わる
        boolean imageMoved = finalImagePath != null && !finalImagePath.equals(requestedImagePath);

        try {
            // 1. MapStructを使用して、Form(DTO)からEntityへ一発で変換
            // （子要素のList<AccountDetailForm>も自動で List<AccountDetail> に変換されます）
            Account account = accountMapper.toEntity(form);

            LocalDateTime now = LocalDateTime.now();

            // save メソッド内で、Accountエンティティに値を詰める際に以下を追加します
            if (form.getCategoryId() != null) {
                AccountCategory category = categoryRepository.findById(form.getCategoryId()).orElse(null);
                account.setAccountCategory(category);
            } else {
                account.setAccountCategory(null);
            }

            // 2. Formには持たせていない、システム側の必須情報をセット
            if (existing != null) {
                // 更新時は、最初の登録情報を引き継ぎ、更新情報だけを書き換える
                account.setEntryUser(existing.getEntryUser());
                account.setEntryDatetime(existing.getEntryDatetime());
                account.setUpdateUser(userName);
                account.setUpdateDatetime(now);
            } else {
                account.setEntryUser(userName);
                account.setEntryDatetime(now);
            }

            // 3. JPAの双方向リレーションシップを解決するため、子から親への参照をセット
            if (account.getDetails() != null) {
                for (AccountDetail detail : account.getDetails()) {
                    detail.setAccount(account);
                    detail.setEntryUser(userName);
                    detail.setEntryDatetime(now);
                }
            }

            // 4. 保存 (CascadeType.ALLにより子も同時に保存される)
            //    DBエラーをこのメソッド内で検知できるよう、その場で反映（flush）する
            accountRepository.saveAndFlush(account);
        } catch (RuntimeException e) {
            // DB 登録に失敗したら、転送済みの画像（S3 など）が孤立しないよう削除する
            if (imageMoved) {
                deleteMovedImageQuietly(finalImagePath, e);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<Account> search(AccountSearchForm form, String userName) {
        // 条件が空の場合でも、他のユーザーのデータが混ざらないよう、必ずログイン中ユーザーで絞り込む
        if (form == null) {
            return accountRepository.findByEntryUserOrderByPurchaseDatetimeDesc(userName);
        }

        // LocalDate を LocalDateTime に変換（nullチェックを含む）
        LocalDateTime startDateTime = null;
        if (form.getStartDate() != null) {
            // 例: 2026-07-01 -> 2026-07-01T00:00:00
            startDateTime = form.getStartDate().atStartOfDay();
        }

        LocalDateTime endDateTime = null;
        if (form.getEndDate() != null) {
            // 例: 2026-07-31 -> 2026-07-31T23:59:59.999999999
            endDateTime = form.getEndDate().atTime(LocalTime.MAX);
        }
        
        return accountRepository.searchAccounts(
                userName,
                startDateTime,
                endDateTime,
                form.getKeyword()
        );
    }

    /**
     * IDから、ログイン中ユーザー本人の家計簿データを取得する。
     * 他のユーザーのデータは「存在しない」場合と同じ扱い（404）にして、IDの存在も知られないようにする。
     */
    @Transactional(readOnly = true)
    public Account findByIdAndOwner(Long id, String userName) {
        return accountRepository.findByAccountIdAndEntryUser(id, userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "該当のデータが見つかりません。"));
    }

    /**
     * IDを指定して、ログイン中ユーザー本人の家計簿データを削除する
     */
    @Transactional
    public void delete(Long id, String userName) {
        // 💡 補足：DBから削除する前に、NAS上の画像も削除したい場合はここで行います
        Account account = findByIdAndOwner(id, userName);
        if (account.getImagePath() != null) {
            fileStorageService.deleteFile(account.getImagePath());
        }

        // DBから該当レコード（ヘッダーと、紐づく明細）を削除
        accountRepository.delete(account);
    }

    /**
     * フォームから届いた画像パスを検証し、DBに保存してよい最終的なパスを返す。
     * <ul>
     *   <li>空 … 画像なし（null）</li>
     *   <li>更新対象の既存の画像パスと同じ … そのまま維持</li>
     *   <li>一時フォルダ（/images/temp/）のパス … 本保存先へ移動して、移動後のパスを返す</li>
     *   <li>上記以外 … 不正な値として400エラー（任意のパスをDBに入れさせない）</li>
     * </ul>
     */
    private String resolveImagePath(String requestedPath, Account existing, String userName) {
        if (requestedPath == null || requestedPath.isBlank()) {
            return null;
        }
        if (existing != null && requestedPath.equals(existing.getImagePath())) {
            return requestedPath;
        }
        if (requestedPath.startsWith(FileStorageService.TEMP_PATH_PREFIX)) {
            try {
                // userName / 日付は、S3版が保存先キー（receipts/{userId}/{yyyy}/{MM}/...）を作るために使う
                return fileStorageService.moveToReceipts(
                        requestedPath, userName, LocalDate.now(ZoneId.of("Asia/Tokyo")));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "画像パスが不正です。", e);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "画像パスが不正です。");
    }

    /**
     * DB登録に失敗したときに、転送済みの画像を削除する。
     * 削除自体に失敗しても、元のエラーを隠さないよう、ログに残して元の例外に付記するだけにする。
     */
    private void deleteMovedImageQuietly(String imagePath, RuntimeException cause) {
        try {
            fileStorageService.deleteFile(imagePath);
        } catch (RuntimeException e) {
            log.warn("DB登録に失敗し、転送済みの画像の削除も失敗しました: {}", imagePath, e);
            cause.addSuppressed(e);
        }
    }

}