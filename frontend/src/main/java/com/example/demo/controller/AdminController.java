package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import com.example.demo.model.RoleName;
import com.example.demo.service.UserService;
import com.example.demo.repository.RoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepo;

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/list";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        var u = userService.findById(id);
        if (u.isEmpty()) {
            return "redirect:/admin/users";
        }
        List<Role> allRoles = roleRepo.findAll();
        model.addAttribute("user", u.get());
        model.addAttribute("allRoles", allRoles);
        // extract role ids for checking boxes
        Set<Long> assigned = u.get().getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        model.addAttribute("assignedRoleIds", assigned);
        return "admin/edit";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id, HttpServletRequest req) {
        var opt = userService.findById(id);
        if (opt.isEmpty()) return "redirect:/admin/users";

        AppUser u = opt.get();
        String[] selected = req.getParameterValues("roles"); // role ids as strings
        Set<Role> newRoles = new HashSet<>();
        if (selected != null) {
            for (String s : selected) {
                try {
                    Long rid = Long.valueOf(s);
                    roleRepo.findById(rid).ifPresent(newRoles::add);
                } catch (NumberFormatException ignored) {}
            }
        }
        u.setRoles(newRoles);
        String enabled = req.getParameter("enabled");
        u.setEnabled("on".equals(enabled) || "true".equalsIgnoreCase(enabled));
        userService.save(u);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/admin/users";
    }
}