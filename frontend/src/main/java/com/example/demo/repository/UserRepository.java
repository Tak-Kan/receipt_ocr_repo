package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    // 💡 ロールIDに紐づくユーザーが存在するかどうかを判定するメソッド
    boolean existsByRole_RoleId(Integer roleId);
}