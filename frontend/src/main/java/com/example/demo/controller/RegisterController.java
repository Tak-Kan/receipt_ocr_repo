package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showForm() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(HttpServletRequest req, Model model) {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String password2 = req.getParameter("password2");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Username and password are required");
            return "register";
        }
        if (!password.equals(password2)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }
        if (userService.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        // default role: ROLE_USER
        userService.createUser(username, password, "ROLE_USER");
        return "redirect:/login?registered";
    }
}