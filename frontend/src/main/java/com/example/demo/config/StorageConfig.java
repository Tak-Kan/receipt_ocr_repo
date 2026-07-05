package com.example.demo.config;

import com.example.demo.service.FileStorageService;
import com.example.demo.service.LocalFileStorageServiceImpl;
import com.example.demo.service.AzureBlobStorageServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}