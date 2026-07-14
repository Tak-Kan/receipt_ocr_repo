package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.Role;
import com.example.demo.repository.RoleRepository2;
import com.example.demo.repository.UserRepository2;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService2 {

    @Autowired
    private UserRepository2 userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}