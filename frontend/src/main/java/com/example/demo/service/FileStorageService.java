package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface FileStorageService {
    /**
     * 一時フォルダに画像を保存する（OCR用）
     */
    String saveToTemp(MultipartFile file);

    /**
     * 一時フォルダから本保存フォルダへファイルを移動・転送する
     */
    String moveToReceipts(String tempImagePath);
}