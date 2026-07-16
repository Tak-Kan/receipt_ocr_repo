package com.example.demo.repository;

import com.example.demo.entity.AccountCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcCategoryRepository extends JpaRepository<AccountCategory, Integer> {
}