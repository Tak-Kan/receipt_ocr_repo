package com.example.demo.controller;

import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showForm() {
        return "hams_user_register";
    }

    @PostMapping("/register")
    public String doRegister(HttpServletRequest req, Model model) {
/*         String username = req.getParameter("username");
        String password = req.getParameter("password");
        String password2 = req.getParameter("password2");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Username and password are required");
            return "hams_user_register";
        }
        if (!password.equals(password2)) {
            model.addAttribute("error", "Passwords do not match");
            return "hams_user_register";
        }
        if (userService.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "hams_user_register";
        }

        // default role: ROLE_USER
        // userService.createUser(username, password, "ROLE_USER");
        userService.createUser(username, password, Set.of(RoleName.USER));
        */
        return "redirect:/login?registered"; 
    }
}