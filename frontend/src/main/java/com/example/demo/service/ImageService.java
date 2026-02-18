package com.example.demo.service;

import com.example.demo.model.ImageEntity;
import com.example.demo.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class ImageService {

    @Autowired
    private ImageRepository repo;

    public ImageEntity save(MultipartFile file) throws Exception {
        ImageEntity e = new ImageEntity();
        e.setFilename(file.getOriginalFilename());
        e.setContentType(file.getContentType());
        e.setSize(file.getSize());
        e.setData(file.getBytes());
        e.setUploadedAt(LocalDateTime.now());
        return repo.save(e);
    }

    public ImageEntity find(Long id) {
        return repo.findById(id).orElse(null);
    }
}