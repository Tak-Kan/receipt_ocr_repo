package com.example.demo;

import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import com.example.demo.model.RoleName;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

@SpringBootApplication
public class DemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Bean
  CommandLineRunner initUsers(UserRepository userRepo, RoleRepository roleRepo, BCryptPasswordEncoder passwordEncoder) {
    return args -> {
      // ensure roles exist
      Role userRole = roleRepo.findByName(RoleName.USER).orElseGet(() -> {
        Role r = new Role();
        r.setName(RoleName.USER);
        return roleRepo.save(r);
      });
      Role adminRole = roleRepo.findByName(RoleName.ADMIN).orElseGet(() -> {
        Role r = new Role();
        r.setName(RoleName.ADMIN);
        return roleRepo.save(r);
      });

      if (userRepo.count() == 0) {
        AppUser u = new AppUser();
        u.setUsername("user");
        u.setPassword(passwordEncoder.encode("password"));
        u.setEnabled(true);
        u.setRoles(Set.of(userRole));
        userRepo.save(u);

        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole, userRole));
        userRepo.save(admin);

        System.out.println("Created default users: user/password and admin/adminpass");
      }
    };
  }
}