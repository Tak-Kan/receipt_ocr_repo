package com.example.demo.controller;

import com.example.demo.dto.AccountForm;
import com.example.demo.dto.AccountDetailForm;
import com.example.demo.dto.OcrResponseDto;
import com.example.demo.service.AccountService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.OcrService;
import com.example.demo.mapper.OcrMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/account")
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

    public AccountController(OcrService ocrService) {
        this.ocrService = ocrService;
        this.fileStorageService = fileStorageService;
        this.ocrMapper = ocrMapper;
    }

    // 画面1: 画像指定画面の表示
    @GetMapping("/upload")
    public String showUploadScreen() {
        return "hams_entry";
    }

    @PostMapping("/read")
    public String readReceipt(@RequestParam("file") MultipartFile file, Model model) {
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
    public String showConfirmScreen(Model model) {
        // FlashAttributeからデータを受け取り、Thymeleafに渡す（空の場合は新規フォーム）
        if (!model.containsAttribute("analyzedData")) {
            return "redirect:/receipt/upload";
        }
        return "hams_confirm";
    }

    @PostMapping("/register")
    public String registerAccount(
            @Validated @ModelAttribute AccountForm accountForm, // @Validatedでチェック実行
            BindingResult bindingResult, // エラー結果がここに格納される
            Authentication auth, //認証情報
            Model model) {
        
        // バリデーションエラーがある場合は元の確認画面へ戻す
        if (bindingResult.hasErrors()) {
            return "confirm"; // confirm.htmlへ戻る（エラーメッセージはThymeleaf側で表示可能）
        }

        // 本登録のタイミングで、tempから本保存先（NAS or クラウド）へ転送！
        // 転送先がどこであっても、メソッドを呼ぶだけで適切なURLパスが返ってきます
        String finalImagePath = fileStorageService.moveToReceipts(accountForm.getReceiptImagePath());
        
        // DBにはこの確定したパスを保存する
        accountForm.setReceiptImagePath(finalImagePath);
        String username = (auth != null) ? auth.getName() : "anonymous";

        // サービス処理へ
        accountService.saveAccount(accountForm, username);

        return "redirect:/top";
    }
}