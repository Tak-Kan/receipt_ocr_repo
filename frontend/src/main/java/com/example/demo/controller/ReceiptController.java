package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // 画面1: 画像指定画面の表示
    @GetMapping("/upload")
    public String showUploadScreen() {
        return "screen1";
    }

    // 画面1: 画像送信処理
    @PostMapping("/analyze")
    public String analyzeImage(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
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
        return "screen2";
    }

    // 画面2: 登録処理
    @PostMapping("/save")
    public String saveReceipt(@RequestParam Map<String, String> formData) {
        // 画面の入力内容をJSON形式でFlaskAPIへ送信
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(formData, headers);
        
        restTemplate.postForEntity(BACKEND_URL + "/receipts", requestEntity, Map.class);
        
        // 登録完了後、とりあえず画面1に戻す（実際は一覧画面等へ）
        return "redirect:/receipt/upload?success";
    }
}