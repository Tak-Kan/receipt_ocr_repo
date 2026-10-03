package com.example.demo.config;

import com.example.demo.service.FileStorageService;
import com.example.demo.service.LocalFileStorageServiceImpl;
import com.example.demo.service.S3FileStorageServiceImpl;
import com.example.demo.service.AzureBlobStorageServiceImpl;
import com.example.demo.service.TempFileStoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class StorageConfig {

    @Bean
    // application.properties で「storage.type=local」の時にこのBean（NAS用）を有効にする
    @ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorageService localFileStorageService() {
        return new LocalFileStorageServiceImpl();
    }

    @Bean
    // 「storage.type=cloud」の時にこのBean（クラウド用）を有効にする
    @ConditionalOnProperty(name = "storage.type", havingValue = "cloud")
    public FileStorageService azureFileStorageService() {
        return new AzureBlobStorageServiceImpl();
    }

    // ---- S3 モード ----

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "aws")
    public S3Client s3Client(@Value("${app.storage.s3.region:ap-northeast-1}") String region) {
        // 認証情報は ~/.aws/credentials（コンテナにマウント）や環境変数から DefaultCredentialsProvider が自動取得
        return S3Client.builder().region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create()) // 自動でファイルを探しにいきます
            .build();
    }

    /** コンテナ内の一時保存領域（NAS マウントの /app/uploads とは別の場所）。 */
    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "aws")
    public TempFileStoreService tempFileStore(
            @Value("${app.storage.temp-dir:/tmp/receipt-temp}") String tempDir) {
        return new TempFileStoreService(Paths.get(tempDir), Duration.ofHours(24));
    }

    @Bean
    // 「storage.type=aws」の時にこのBean（AWS用）を有効にする
    @ConditionalOnProperty(name = "storage.type", havingValue = "aws")
    public FileStorageService s3FileStorageService(
            S3Client s3Client,
            TempFileStoreService tempFileStore,
            @Value("${app.storage.s3.bucket}") String bucket) {
        return new S3FileStorageServiceImpl(s3Client, bucket, tempFileStore);
    }
}