package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path tempDir = Paths.get("/app/uploads/temp");
    private final Path receiptsDir = Paths.get("/app/uploads/receipts"); // ここがNASにマウントされている想定
    // WebMvcConfigのルーティングに合わせたプレフィックス
    private static final String DB_PATH_PREFIX = "/images/";
    // Dockerコンテナ内のマウント先（NASの実体）
    private static final String LOCAL_BASE_DIR = "/app/uploads/";

    @Override
    public String saveToTemp(MultipartFile file) {
        try {
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            // ファイル名の重複を防ぐためUUIDを付与
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetPath = tempDir.resolve(fileName);
            
            Files.copy(file.getInputStream(), targetPath);
            
            // 画面表示やDB格納用の「Webから見たパス」を返す
            return "/images/temp/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("一時フォルダへの保存に失敗しました", e);
        }
    }

    @Override
    public String moveToReceipts(String tempImagePath) {
        try {
            // Web用のパス（/images/temp/xxx.jpg）から、実際のファイル名（xxx.jpg）を抽出
            String fileName = tempImagePath.substring(tempImagePath.lastIndexOf("/") + 1);
            
            Path sourcePath = tempDir.resolve(fileName);
            Path targetPath = receiptsDir.resolve(fileName);

            if (!Files.exists(receiptsDir)) {
                Files.createDirectories(receiptsDir);
            }

            // temp から receipts(NAS) へ移動
            Files.move(sourcePath, targetPath);

            // 本保存後のWeb用パスを返す
            return "/images/receipts/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("NASへのファイル転送に失敗しました", e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        // DBのパス "/images/xxx.jpg" をコンテナの物理パス "/app/uploads/xxx.jpg" に変換
        String actualPathString = filePath.replaceFirst(DB_PATH_PREFIX, LOCAL_BASE_DIR);
        Path physicalPath = Paths.get(actualPathString);

        try {
            // ファイルが存在する場合のみ削除（存在しない場合は何もしない安全なメソッド）
            boolean deleted = Files.deleteIfExists(physicalPath);
            if (deleted) {
                log.info("ローカルファイルを削除しました: {}", physicalPath);
            }
        } catch (IOException e) {
            // 権限エラーや使用中ロックなどで消せない場合は例外を投げるか、ログのみ残す
            log.error("ローカルファイルの削除に失敗しました: {}", physicalPath, e);
            throw new RuntimeException("ファイルの削除に失敗しました", e);
        }
    }
}