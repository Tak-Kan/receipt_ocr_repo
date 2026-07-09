package com.example.demo.controller;

import com.example.demo.dto.AccountForm;
import com.example.demo.dto.AccountDetailForm;
import com.example.demo.dto.AccountSearchForm;
import com.example.demo.dto.OcrResponseDto;
import com.example.demo.entity.Account;
import com.example.demo.service.AccountService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.OcrService;
import com.example.demo.mapper.AccountMapper;
import com.example.demo.mapper.OcrMapper;
import com.example.demo.validation.OnSave;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private FileStorageService fileStorageService; // 設定ファイルに応じて、LocalかAzureが自動で入る
    // 作成したOcrServiceを注入
    @Autowired
    private OcrService ocrService;
    @Autowired
    private OcrMapper ocrMapper; // 作成したMapperをDI
    @Autowired
    private AccountMapper accountMapper;

    public AccountController(OcrService ocrService) {
        this.ocrService = ocrService;
        this.fileStorageService = fileStorageService;
        this.ocrMapper = ocrMapper;
    }

    // 画面1: 画像指定画面の表示
    @GetMapping("/upload")
    public String showUploadScreen(Authentication auth, Model model) {
        String username = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("username", username);
        return "hams_entry";
    }

    @PostMapping("/read")
    public String readReceipt(@RequestParam("file") MultipartFile file, 
            Authentication auth, //認証情報
            Model model) {
                
        String username = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("username", username);

        try{
            // 2. 【今回のポイント】画像を一時フォルダ（temp）へコピー・保存する
            // 内部で UUID を付与した一意のファイル名（例: 550e8400-e29b..._receipt.jpg）として
            // サーバーの /app/uploads/temp/ の中にファイルがコピーされます。
            // 戻り値として Web表示用のパス「/images/temp/UUID_ファイル名」が返ってきます。
            String tempImagePath = fileStorageService.saveToTemp(file);

            // 1. PythonのOCR APIへ画像を送信し、OcrResponseDtoで結果を受け取る（※RestTemplate等の実装詳細は割愛）
            OcrResponseDto ocrResult = ocrService.analyzeReceipt(file); 

            // 2. Mapperを使って一発でFormに変換（金額のクレンジングや日時の結合も自動で実行されます）
            AccountForm form = ocrMapper.toForm(ocrResult);

            // 画像パスは一旦仮置き（ローカルに保存したパスをセットしてください）
            form.setReceiptImagePath(tempImagePath);

            model.addAttribute("accountForm", form);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "読み取り処理に失敗しました: " + e.getMessage());
            return "hams_entry";
        }
        // 3. 読み取り確認画面へ遷移
        // 画面2へリダイレクト
        return "hams_confirm";
    }

    // 画面2: 結果表示・編集画面の表示
    @GetMapping("/confirm")
    public String showConfirmScreen(Authentication auth, Model model) {
        String username = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("username", username);
        // FlashAttributeからデータを受け取り、Thymeleafに渡す（空の場合は新規フォーム）
        if (!model.containsAttribute("analyzedData")) {
            return "redirect:/receipt/upload";
        }
        return "hams_confirm";
    }

    @PostMapping("/register")
    public String registerAccount(
            // 💡 ここで OnSave グループのバリデーションを実行！
            @Validated(OnSave.class) @ModelAttribute AccountForm accountForm, // @Validatedでチェック実行
            BindingResult bindingResult, // エラー結果がここに格納される
            Authentication auth, //認証情報
            Model model) {
        
        // バリデーションエラーがある場合は元の確認画面へ戻す
        if (bindingResult.hasErrors()) {
            return "hams_confirm"; // hams_confirm.htmlへ戻る（エラーメッセージはThymeleaf側で表示可能）
        }

        // 本登録のタイミングで、tempから本保存先（NAS or クラウド）へ転送！
        // 転送先がどこであっても、メソッドを呼ぶだけで適切なURLパスが返ってきます        
        String currentImagePath = accountForm.getReceiptImagePath();

        // パスに "temp" が含まれている場合のみ、本番ディレクトリへ移動する
        if (currentImagePath != null && currentImagePath.contains("/temp/")) {
            String finalImagePath = fileStorageService.moveToReceipts(currentImagePath);
            // DBにはこの確定したパスを保存する
            accountForm.setReceiptImagePath(finalImagePath);
        }
        
        String username = (auth != null) ? auth.getName() : "anonymous";

        // サービス処理へ
        accountService.saveAccount(accountForm, username);

        return "redirect:/top";
    }

    // 検索画面の表示と検索実行
    @GetMapping("/search")
    public String search(@ModelAttribute("searchForm") AccountSearchForm searchForm, Authentication auth, Model model) {

        String username = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("username", username);

        // サービスを呼び出して検索を実行（初回アクセス時は全件表示や今月分のみ表示などに調整可能）
        List<Account> accountList = accountService.search(searchForm, username);
        
        // 画面に検索結果を渡す
        model.addAttribute("accountList", accountList);
        
        return "hams_search"; // src/main/resources/templates/hams_search.html
    }


    /**
     * 詳細（編集）画面の表示
     */
    @GetMapping("/detail/{id}")
    public String showEdit(@PathVariable("id") Long id, Authentication auth, Model model) {
        String username = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("username", username);
        // 1. DBからデータを取得
        Account account = accountService.findById(id);
        
        // 2. Entity を Form に変換
        AccountForm accountForm = accountMapper.toForm(account);
        
        // 3. 画面へ渡す
        model.addAttribute("accountForm", accountForm);
        
        return "hams_confirm";
    }

    /**
     * データの削除処理
     */
    @PostMapping("/delete")
    public String delete(@RequestParam("accountId") Long accountId) {
        // フォーム内の隠し項目（<input type="hidden" th:field="*{accountId}">）
        // の値だけを受け取って削除処理へ渡す
        accountService.delete(accountId);
        
        return "redirect:/account/search";
    }

}