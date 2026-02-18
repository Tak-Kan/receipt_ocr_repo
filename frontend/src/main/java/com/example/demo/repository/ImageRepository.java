package com.example.demo.repository;

import com.example.demo.model.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
    // 追加の検索が必要ならここに定義
}