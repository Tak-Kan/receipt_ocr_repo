// AdminRoleController.java
package com.example.demo.controller;

import com.example.demo.dto.RoleForm;
import com.example.demo.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAllRoles());
        return "admin/role_list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        RoleForm form = new RoleForm();
        form.setNew(true);
        model.addAttribute("roleForm", form);
        return "admin/role_form";
    }

    @GetMapping("/{roleId}/edit")
    public String editForm(@PathVariable Integer roleId, Model model) {
        model.addAttribute("roleForm", roleService.getRoleForm(roleId));
        return "admin/role_form";
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute RoleForm roleForm, Authentication auth, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/role_form";
        }
        
        String userName = (auth != null) ? auth.getName() : "anonymous";

        try {
            roleService.save(roleForm, userName);
        } catch (IllegalArgumentException e) {
            result.rejectValue("authEntryFlag", "", e.getMessage());
            return "admin/role_form";
        }

        return "redirect:/admin/roles";
    }

    @PostMapping("/{roleId}/delete")
    public String delete(@PathVariable Integer roleId, RedirectAttributes redirectAttributes) {
        try {
            roleService.delete(roleId);
        } catch (Exception e) {
            // 削除エラー時（使用中など）は一覧画面にエラーメッセージを表示してリダイレクト
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }
}