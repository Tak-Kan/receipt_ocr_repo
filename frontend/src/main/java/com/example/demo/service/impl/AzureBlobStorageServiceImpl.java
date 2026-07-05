package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AzureBlobStorageServiceImpl implements FileStorageService {

    // private final BlobServiceClient blobServiceClient; // AzureのSDKクライアントなど

    @Override
    public String saveToTemp(MultipartFile file) {
        // 一時保存は今まで通りローカルのtempで行う
        return "（Localと同様の処理）";
    }

    @Override
    public String moveToReceipts(String tempImagePath) {
        // ★ここを書き換える
        // 1. ローカルのtempフォルダから該当ファイルを読み込む
        // 2. Azure Blob Storage の SDK を使ってクラウドへアップロードする
        // 3. アップロード成功後、クラウド上の公開URL（https://...）を取得する
        // 4. ローカルのtempファイルを削除する
        
        // return "https://mystorage.blob.core.windows.net/receipts/xxx.jpg";
        return null;
    }
}