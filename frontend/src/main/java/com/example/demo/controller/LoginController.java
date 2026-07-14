// LoginController.java
package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // SecurityConfigの .loginPage("/login") と一致させます
    @GetMapping("/login")
    public String showLoginForm() {
        //return "login/login-form"; // ログイン画面のHTMLファイル名
        return "hams_login"; // ログイン画面のHTMLファイル名
    }
}