package com.example.demo.service;

import com.example.demo.model.UserRole;
import com.example.demo.model.RoleName;
import com.example.demo.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepo;

    public List<UserRole> findAll() {
        return roleRepo.findAll();
    }

    public Optional<UserRole> findById(Long id) {
        return roleRepo.findById(id);
    }

    public Optional<UserRole> findByName(RoleName name) {
        return roleRepo.findByName(name);
    }

    public UserRole create(RoleName name) {
        return roleRepo.findByName(name).orElseGet(() -> {
            UserRole r = new UserRole();
            r.setName(name);
            return roleRepo.save(r);
        });
    }

    public void deleteById(Long id) {
        roleRepo.deleteById(id);
    }

    /**
     * Return RoleName values that are not yet present in DB (for the "create" dropdown).
     */
    public List<RoleName> availableRoleNames() {
        List<RoleName> existing = roleRepo.findAll().stream()
                .map(UserRole::getName)
                .collect(Collectors.toList());
        return Arrays.stream(RoleName.values())
                .filter(rn -> !existing.contains(rn))
                .collect(Collectors.toList());
    }
}