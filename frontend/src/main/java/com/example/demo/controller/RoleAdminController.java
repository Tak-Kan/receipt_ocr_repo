package com.example.demo.controller;

import com.example.demo.model.RoleName;
import com.example.demo.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleAdminController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public String listRoles(Model model, @RequestParam(value = "msg", required = false) String msg) {
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("availableNames", roleService.availableRoleNames());
        if (msg != null) model.addAttribute("msg", msg);
        return "admin/roles";
    }

    @PostMapping
    public String createRole(HttpServletRequest req, Model model) {
        String roleName = req.getParameter("roleName");
        if (roleName == null || roleName.isBlank()) {
            return "redirect:/admin/roles?msg=invalid";
        }
        try {
            RoleName rn = RoleName.valueOf(roleName);
            roleService.create(rn);
            return "redirect:/admin/roles?msg=created";
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/roles?msg=invalid";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteRole(@PathVariable Long id) {
        roleService.deleteById(id);
        return "redirect:/admin/roles?msg=deleted";
    }
}