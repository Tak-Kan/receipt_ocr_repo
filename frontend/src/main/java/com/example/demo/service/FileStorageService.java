package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.time.LocalDate;

public interface FileStorageService {

    /** 一時保存した画像のWeb用パスの先頭部分（saveToTempが返すパスはこれで始まる） */
    String TEMP_PATH_PREFIX = "/images/temp/";

    /**
     * 一時フォルダに画像を保存する（OCR用）
     */
    String saveToTemp(MultipartFile file);

    /**
     * 一時フォルダから本保存フォルダへファイルを移動・転送する
     */
    String moveToReceipts(String tempImagePath, String userId, LocalDate date);

    /**
     * 本保存フォルダからファイルを削除する
     */
    void deleteFile(String imagePath);
}