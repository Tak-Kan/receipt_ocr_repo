package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public AppUser createUser(String username, String rawPassword, String roles) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRoles(roles);
        u.setEnabled(true);
        return repo.save(u);
    }

    public Optional<AppUser> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    public List<AppUser> findAll() {
        return repo.findAll();
    }

    public Optional<AppUser> findById(Long id) {
        return repo.findById(id);
    }

    public AppUser save(AppUser user) {
        return repo.save(user);
    }

    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}