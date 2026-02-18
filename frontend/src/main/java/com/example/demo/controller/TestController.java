package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.model.RoleName;
import com.example.demo.model.ImageEntity;
import com.example.demo.service.UserService;
import com.example.demo.service.ImageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Controller
public class TestController {

    @Autowired
    private UserService userService;
    @Autowired
    private ImageService imageService;

    @GetMapping("/testPage")
    public String showForm() {
        return "testPage";
    }

    @PostMapping("/testPage")
    public String doRegister(HttpServletRequest req, Model model) {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String password2 = req.getParameter("password2");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Username and password are required");
            return "testPage";
        }
        if (!password.equals(password2)) {
            model.addAttribute("error", "Passwords do not match");
            return "testPage";
        }
        if (userService.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "testPage";
        }

        // default role: ROLE_USER
        // userService.createUser(username, password, "ROLE_USER");
        userService.createUser(username, password, Set.of(RoleName.USER));
        return "redirect:/login?registered";
    }

    // Handle upload
    @PostMapping("/testImport")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "No file selected");
            return "testPage";
        }
        // Optionally: validate content type
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            model.addAttribute("error", "Please upload an image file");
            return "testPage";
        }
        try {
            ImageEntity saved = imageService.save(file);
            model.addAttribute("success", "Uploaded id=" + saved.getId());
            model.addAttribute("uploadedId", saved.getId());
        } catch (Exception e) {
            model.addAttribute("error", "Failed to save file: " + e.getMessage());
            return "testPage";
        }
        return "testPage";
    }
}