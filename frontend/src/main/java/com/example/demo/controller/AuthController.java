package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private static final String SESSION_USER_KEY = "user";

    // top page (requires login)
    public String top(HttpServletRequest req, Model model) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(SESSION_USER_KEY) == null) {
            return "redirect:/login";
        }
        String userName = (String) session.getAttribute(SESSION_USER_KEY);
        model.addAttribute("userName", userName);
        return "hams_top";
    }

    // logout
    @GetMapping("/logout")
    public String logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}