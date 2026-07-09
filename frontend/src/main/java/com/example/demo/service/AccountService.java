package com.example.demo.service;

import com.example.demo.dto.AccountForm;
import com.example.demo.dto.AccountSearchForm;
import com.example.demo.entity.Account;
import com.example.demo.entity.AccountDetail;
import com.example.demo.mapper.AccountMapper;
import com.example.demo.repository.AccountRepository;
import com.example.demo.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

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
        // 1. MapStructを使用して、Form(DTO)からEntityへ一発で変換
        // （子要素のList<AccountDetailForm>も自動で List<AccountDetail> に変換されます）
        Account account = accountMapper.toEntity(form);

        LocalDateTime now = LocalDateTime.now();
        
        // 2. Formには持たせていない、システム側の必須情報をセット
        account.setEntryUser(userName);
        account.setEntryDatetime(now);

        // 3. JPAの双方向リレーションシップを解決するため、子から親への参照をセット
        if (account.getDetails() != null) {
            for (AccountDetail detail : account.getDetails()) {
                detail.setAccount(account);
                detail.setEntryUser(userName);
                detail.setEntryDatetime(now);
            }
        }

        // 4. 保存 (CascadeType.ALLにより子も同時に保存される)
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> search(AccountSearchForm form, String userName) {
        // 条件が空の初期表示時などは全件検索とするか、Repository側で対応するか制御します
        if (form == null) {
            return accountRepository.findAll();
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
     * IDから家計簿データを取得する
     */
    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("該当のデータが見つかりません。ID: " + id));
    }

    /**
     * IDを指定して家計簿データを削除する
     */
    @Transactional
    public void delete(Long id) {
        // 💡 補足：DBから削除する前に、NAS上の画像も削除したい場合はここで行います
        Account account = findById(id);
        if (account.getImagePath() != null) {
            fileStorageService.deleteFile(account.getImagePath());
        }
        
        // DBから該当レコード（ヘッダーと、紐づく明細）を削除
        accountRepository.deleteById(id);
    }

}