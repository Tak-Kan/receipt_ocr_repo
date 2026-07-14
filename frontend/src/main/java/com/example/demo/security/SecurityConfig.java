package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider; // 💡 追加
import org.springframework.security.crypto.password.PasswordEncoder; // 💡 追加
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Configuration
@EnableWebSecurity // Spring Securityを有効化する
@EnableMethodSecurity // メソッド単位での権限チェック(@PreAuthorizeなど)を有効化する
public class SecurityConfig {
    // 💡 新しく追加するメソッド：Spring Securityに私たちの自作クラスを強制認識させます
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsServiceImpl userDetailsService, 
            PasswordEncoder passwordEncoder) {
        
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // ここで「DB検索処理」と「パスワード暗号化ツール」をガッチリとセットします
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        
        return provider;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. URLごとのアクセス制御設定
        http.authorizeHttpRequests(authz -> authz
                // CSS, JS, 画像などの静的リソースはログイン前でも見れるように許可
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/error").permitAll()
                // 💡 今後作る管理者専用画面（/admin/～）は、"ROLE_ADMIN" 権限を持つ人のみアクセス可能にする
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                
                // それ以外のすべてのURL（家計簿の検索や登録など）は、ログインしている人なら誰でもアクセス可能
                .anyRequest().authenticated()
        );

        // 2. ログイン機能の設定
        http.formLogin(login -> login
                // 自作のログイン画面のURL（後で作成します）
                .loginPage("/login")
                
                // ログイン処理を送信する先のURL（Spring Securityが自動で処理するため、Controllerには作りません）
                .loginProcessingUrl("/login-process")
                
                // ログイン画面のHTMLで設定する「ログインID」のname属性（今回はユーザーコード）
                .usernameParameter("userCode")
                
                // ログイン画面のHTMLで設定する「パスワード」のname属性
                .passwordParameter("password")
                
                // ログイン成功後に遷移するURL（trueにすると、直前に見ていたページを無視して常にここへ飛びます）
                .defaultSuccessUrl("/top", true)
                
                // ログイン失敗時の遷移先（?error=true が付くことで、HTML側でエラーメッセージを出せます）
                .failureUrl("/login?error=true")
                
                // ログイン画面自体はログイン前でも見れるように許可
                .permitAll()
        );

        // 3. ログアウト機能の設定
        http.logout(logout -> logout
                // ログアウト処理を送信する先のURL
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                
                // ログアウト成功後に遷移するURL（ログイン画面に戻す）
                .logoutSuccessUrl("/login?logout=true")
                
                // ログアウト後にセッション情報（誰がログインしていたかの記憶）を破棄する
                .invalidateHttpSession(true)
                
                // ログアウトも誰でもアクセス許可
                .permitAll()
        );

        // 注意：H2データベース等を利用している場合は CSRF 設定の無効化等が必要になりますが、
        // 今回はMySQLを利用している想定のため、デフォルトで安全な CSRF（クロスサイトリクエストフォージェリ）対策が有効になっています。

        return http.build();
    }

}