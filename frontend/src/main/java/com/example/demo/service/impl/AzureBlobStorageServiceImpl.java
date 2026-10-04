package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class AzureBlobStorageServiceImpl implements FileStorageService {

    // private final BlobServiceClient blobServiceClient; // AzureのSDKクライアントなど
    private static final String DB_PATH_PREFIX = "/images/";

    // 接続クライアントとコンテナ名（例: "receipts-container"）をDIで受け取る想定
    // public AzureFileStorageServiceImpl(BlobServiceClient blobServiceClient, String containerName) {
    //     this.blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
    // }

    @Override
    public String saveToTemp(MultipartFile file) {
        // 一時保存は今まで通りローカルのtempで行う
        return "（Localと同様の処理）";
    }

    @Override
    public String moveToReceipts(String tempImagePath, String userId, LocalDate date) {
        // ★ここを書き換える
        // 1. ローカルのtempフォルダから該当ファイルを読み込む
        // 2. Azure Blob Storage の SDK を使ってクラウドへアップロードする
        // 3. アップロード成功後、クラウド上の公開URL（https://...）を取得する
        // 4. ローカルのtempファイルを削除する
        
        // return "https://mystorage.blob.core.windows.net/receipts/xxx.jpg";
        return null;
    }

    @Override
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        // Azure Blobのオブジェクトキーには先頭の "/images/" が不要な場合が多いため除去
        // 例: "/images/2026/07/receipt.jpg" -> "2026/07/receipt.jpg"
        // String blobName = filePath.replaceFirst(DB_PATH_PREFIX, "");

        try {
            // BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            
            // Blobが存在する場合のみ削除
            // boolean deleted = blobClient.deleteIfExists();
            // if (deleted) {
            //     log.info("Azure Blobからファイルを削除しました: {}", blobName);
            // }
        } catch (Exception e) {
            // log.error("Azure Blobのファイル削除に失敗しました: {}", blobName, e);
            throw new RuntimeException("クラウド上のファイル削除に失敗しました", e);
        }
    }
}