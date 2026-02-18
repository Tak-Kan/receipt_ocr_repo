package com.example.demo.controller;

import com.example.demo.model.ImageEntity;
import com.example.demo.service.ImageService;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/image")
public class ImageController {

    @Autowired
    private ImageService imageService;

    // Upload form
    @GetMapping("/upload")
    public String uploadForm(Authentication auth, Model model) {
        String username = (auth != null) ? auth.getName() : "anonymous";
        model.addAttribute("username", username);
        return "image_import";
    }

    // Handle upload
    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "No file selected");
            return "image_import";
        }
        // Optionally: validate content type
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            model.addAttribute("error", "Please upload an image file");
            return "image_import";
        }
        try {
            ImageEntity saved = imageService.save(file);
            model.addAttribute("success", "Uploaded id=" + saved.getId());
            model.addAttribute("uploadedId", saved.getId());
        } catch (Exception e) {
            model.addAttribute("error", "Failed to save file: " + e.getMessage());
            return "image_import";
        }
        return "image_info";
    }

    // Serve image by id (useful for preview)
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> serveImage(@PathVariable Long id) {
        ImageEntity e = imageService.find(id);
        if (e == null) return ResponseEntity.notFound().build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(e.getContentType() == null ? "application/octet-stream" : e.getContentType()));
        headers.setContentLength(e.getSize() == null ? e.getData().length : e.getSize());
        return new ResponseEntity<>(e.getData(), headers, HttpStatus.OK);
    }
}