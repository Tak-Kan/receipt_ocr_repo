package com.example.demo;

import com.example.demo.model.AppUser;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class DemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  // 初期ユーザを作成（存在しない場合）: 一般ユーザ + 管理者
  @Bean
  CommandLineRunner initUsers(UserRepository repo, BCryptPasswordEncoder passwordEncoder) {
    return args -> {
      if (repo.count() == 0) {
        AppUser u = new AppUser();
        u.setUsername("user");
        u.setPassword(passwordEncoder.encode("password"));
        u.setRoles("ROLE_USER");
        u.setEnabled(true);
        repo.save(u);

        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRoles("ROLE_ADMIN,ROLE_USER");
        admin.setEnabled(true);
        repo.save(admin);

        System.out.println("Created default users: user/password and admin/adminpass");
      }
    };
  }
}