package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/top")
    public String topPage(Authentication auth, Model model) {
        String userName = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("userName", userName);
        return "hams_top";
    }
}