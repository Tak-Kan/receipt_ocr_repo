package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @Value("${spring.datasource.url:unknown}")
    private String datasourceUrl;

    @GetMapping("/hello")
    public Map<String, String> hello() {
        // シンプルに DB の接続先情報を返す（実際は直接 DB にアクセスする処理を追加できます）
        return Map.of(
            "message", "Hello from Spring Boot!",
            "datasource", datasourceUrl
        );
    }
}