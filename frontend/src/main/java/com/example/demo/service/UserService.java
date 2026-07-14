package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.UserRole;
import com.example.demo.model.RoleName;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public AppUser createUser(String username, String rawPassword, Set<RoleName> roleNames) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setEnabled(true);
        u.setRoles(resolveRoles(roleNames));
        return repo.save(u);
    }

    public Set<UserRole> resolveRoles(Set<RoleName> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            roleNames = Set.of(RoleName.USER);
        }
        return roleNames.stream()
                .map(rn -> roleRepo.findByName(rn)
                        .orElseGet(() -> {
                            UserRole r = new UserRole();
                            r.setName(rn);
                            return roleRepo.save(r);
                        }))
                .collect(Collectors.toSet());
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