package com.example.demo.repository;

import com.example.demo.model.UserRole;
import com.example.demo.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByName(RoleName name);
}