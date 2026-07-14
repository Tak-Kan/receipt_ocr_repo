package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.entity.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DataSeedConfig {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 💡 アプリ起動時に自動実行される処理
    @Bean
    public CommandLineRunner initAdminUser(PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. ロールの初期データ作成
            if (roleRepository.count() == 0) {
                // 管理者ロール（登録・削除どちらも可能）
                Role adminRole = new Role();
                adminRole.setRoleId(1);
                adminRole.setRoleName("管理者");
                adminRole.setAuthEntryFlag(true);
                adminRole.setAuthDeleteFlag(true);
                adminRole.setEntryUser("SYSTEM");
                adminRole.setEntryDatetime(LocalDateTime.now());
                roleRepository.save(adminRole);

                // 一般ロール（登録はできるが、削除はできない）
                Role userRole = new Role();
                userRole.setRoleId(2);
                userRole.setRoleName("一般");
                userRole.setAuthEntryFlag(true);
                userRole.setAuthDeleteFlag(false);
                userRole.setEntryUser("SYSTEM");
                userRole.setEntryDatetime(LocalDateTime.now());
                roleRepository.save(userRole);
                
                System.out.println("★ ロールの初期データを作成しました！");
            }

            // すでにユーザーが存在する場合は何もしない
            if (userRepository.count() == 0) {
                Role adminRole = roleRepository.findById(1).orElseThrow();

                User admin = new User();
                admin.setUserCode("admin");
                admin.setUserName("テスト管理者");
                // "password" という文字列を暗号化してセット
                admin.setPassword(passwordEncoder.encode("adminpassword")); 
                //admin.setEmail("admin@example.com");
                admin.setRole(adminRole);
                admin.setEntryUser("SYSTEM");
                admin.setEntryDatetime(LocalDateTime.now());
                
                userRepository.save(admin);
                System.out.println("★ 初期管理者ユーザー（admin / adminpassword）を作成しました！");
            }
        };
    }
}