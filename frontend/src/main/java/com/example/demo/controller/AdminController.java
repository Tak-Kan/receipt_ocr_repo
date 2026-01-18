package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

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
        model.addAttribute("user", u.get());
        return "admin/edit";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id, HttpServletRequest req) {
        var opt = userService.findById(id);
        if (opt.isEmpty()) return "redirect:/admin/users";

        AppUser u = opt.get();
        String roles = req.getParameter("roles");
        String enabled = req.getParameter("enabled");
        u.setRoles(roles == null ? "" : roles);
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