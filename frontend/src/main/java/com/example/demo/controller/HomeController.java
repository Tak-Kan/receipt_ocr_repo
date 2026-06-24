package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/login"})
    public String loginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/top";
        }
        return "hams_login";
    }

    @GetMapping("/top")
    public String topPage(Authentication auth, Model model) {
        String username = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("username", username);
        return "hams_top";
    }
}