package com.example.demo.controller;

import com.example.demo.model.ImageEntity;
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
    private ImageService imageService;

    @GetMapping("/testPage")
    public String showForm() {
        return "testPage";
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