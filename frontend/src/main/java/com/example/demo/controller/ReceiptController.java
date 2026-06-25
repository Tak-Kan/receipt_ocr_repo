package com.example.demo.controller;

import com.example.demo.model.TAccount;
import com.example.demo.repository.TAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.List;

@Controller
@RequestMapping("/receipt")
public class ReceiptController {

    //private final String BACKEND_URL = "http://localhost:5000/api"; // Docker環境なら "http://backend:5000/api"
    private final String BACKEND_URL = "http://backend:5000/api";
    //@Value("${BACKEND_API_URL:http://backend:5000/api}")
    //private final String BACKEND_URL;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private TAccountRepository tAccountRepository; // Repositoryをインジェクション

    // 画面1: 画像指定画面の表示
    @GetMapping("/upload")
    public String showUploadScreen() {
        return "hams_entry";
    }

    // 画面1: 画像送信処理
    @PostMapping("/analyze")
    public String analyzeImage(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            // 【追加】画像をBase64文字列に変換して次の画面に引き継ぐ
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            redirectAttributes.addFlashAttribute("imageBase64", base64Image);
            redirectAttributes.addFlashAttribute("imageType", file.getContentType()); // image/jpeg 等

            // FlaskAPIへファイルを送信するための準備
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource()); // MultipartFileをリソースとしてセット

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Flaskの解析APIを呼び出し
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    BACKEND_URL + "/analyze-receipt", requestEntity, Map.class);

            // 解析結果のJSON（Mapに変換されたもの）を次の画面に引き継ぐ
            redirectAttributes.addFlashAttribute("analyzedData", response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/receipt/upload?error";
        }

        // 画面2へリダイレクト
        return "redirect:/receipt/confirm";
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

    // 画面2: 登録処理
    @PostMapping("/save")
    public String saveReceipt(@RequestParam("storeName") String storeName,
                              @RequestParam("date") String dateStr,
                              @RequestParam("time") String timeStr,
                              @RequestParam("amount") Integer amount) {

    
        TAccount taccount = new TAccount();
        
        // ★ 日付と時刻を結合して LocalDateTime に変換する処理 ★
        if (dateStr != null && !dateStr.isEmpty() && timeStr != null && !timeStr.isEmpty()) {
            // パターンを指定してフォーマッターを作成
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
            // "2026-06-24" を LocalDate に
            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            // "15:30" を LocalTime に
            LocalTime time = LocalTime.parse(timeStr);
            // ２つを結合して LocalDateTime (2026-06-24T15:30) に
            LocalDateTime dateTime = LocalDateTime.of(date, time);
            
            taccount.setPurchaseDatetime(dateTime);
        }

        LocalDateTime now = LocalDateTime.now();
        taccount.setStoreName(storeName);
        taccount.setTotalAmount(amount);
        taccount.setEntryDatetime(now);
        taccount.setEntryUser("testuser");

        // DBに保存
        tAccountRepository.save(taccount);

        // restTemplate.postForEntity(BACKEND_URL + "/receipts", requestEntity, Map.class);
        
        // 登録完了後、とりあえず画面1に戻す（実際は一覧画面等へ）
        // return "redirect:/receipt/search";
        // 登録完了後、とりあえず画面1に戻す（実際は一覧画面等へ）
        return "redirect:/receipt/upload?success";
    }

}