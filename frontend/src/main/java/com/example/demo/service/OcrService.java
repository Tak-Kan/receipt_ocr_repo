package com.example.demo.service;

import com.example.demo.dto.OcrResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class OcrService {

    private final RestTemplate restTemplate;

    @Value("${OCR_API_URL:http://backend:5000/api/analyze-receipt}")
    private String ocrApiUrl;

    public OcrService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * PythonのOCR APIを呼び出し、解析結果をDTOで返します。
     * * @param file 読み取るレシート画像ファイル
     * @return OcrResponseDto (解析結果)
     */
    public OcrResponseDto analyzeReceipt(MultipartFile file) {
        try {
            // FlaskAPIへファイルを送信するための準備
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // ファイル名付きのByteArrayResourceを作成
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            body.add("file", fileAsResource); // MultipartFileをリソースとしてセット

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // POSTリクエストを送信
            ResponseEntity<OcrResponseDto> response = restTemplate.postForEntity(
                    ocrApiUrl,
                    requestEntity,
                    OcrResponseDto.class
            );

            return response.getBody();

        } catch (IOException e) {
            throw new RuntimeException("画像ファイルの読み込みに失敗しました。", e);
        } catch (Exception e) {
            throw new RuntimeException("OCRサーバーとの通信、または解析処理に失敗しました: " + e.getMessage(), e);
        }
    }
}