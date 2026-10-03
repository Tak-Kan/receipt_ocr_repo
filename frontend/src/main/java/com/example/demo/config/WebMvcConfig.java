package com.example.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        // 【設定内容】
        // ブラウザから「/images/○○○」というURLでアクセスされた場合、
        // サーバー（Dockerコンテナ内）の「/app/uploads/○○○」というファイルを見に行くように紐付けます。
        
        registry.addResourceHandler("/images/**")
                // file: プレフィックスをつけることで、ファイルシステムの絶対パスとして認識させます。
                // 最後にスラッシュ(/)を忘れないように注意してください。
                .addResourceLocations("file:/app/uploads/");
    }
}