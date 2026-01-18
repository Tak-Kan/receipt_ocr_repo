package com.example.demo.service;

import com.example.demo.model.Role;
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

    public List<Role> findAll() {
        return roleRepo.findAll();
    }

    public Optional<Role> findById(Long id) {
        return roleRepo.findById(id);
    }

    public Optional<Role> findByName(RoleName name) {
        return roleRepo.findByName(name);
    }

    public Role create(RoleName name) {
        return roleRepo.findByName(name).orElseGet(() -> {
            Role r = new Role();
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
                .map(Role::getName)
                .collect(Collectors.toList());
        return Arrays.stream(RoleName.values())
                .filter(rn -> !existing.contains(rn))
                .collect(Collectors.toList());
    }
}