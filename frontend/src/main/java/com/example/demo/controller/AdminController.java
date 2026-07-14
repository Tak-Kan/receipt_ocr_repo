package com.example.demo.controller;

import com.example.demo.dto.UserForm;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // 💡 管理者権限を要求
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/user_list";
    }

    // 新規登録画面（フォーム表示）
    @GetMapping("/users/new")
    public String createForm(Model model) {
        UserForm form = new UserForm();
        form.setNew(true); // 新規モードフラグをON
        model.addAttribute("userForm", form);
        model.addAttribute("roles", userService.findAllRoles());
        return "admin/user_form";
    }

    // 編集画面（フォーム表示）
    @GetMapping("/users/{userCode}/edit")
    public String editForm(@PathVariable String userCode, Model model) {
        model.addAttribute("userForm", userService.getUserForm(userCode));
        model.addAttribute("roles", userService.findAllRoles());
        return "admin/user_form";
    }

    // 保存処理
    @PostMapping("/users/save")
    public String save(@Validated @ModelAttribute UserForm userForm, BindingResult result, Model model) {
        // バリデーションエラーがある場合は元の画面に戻す
        if (result.hasErrors()) {
            model.addAttribute("roles", userService.findAllRoles());
            return "admin/user_form";
        }
        
        // 新規作成時、パスワードが空なら手動でエラーを追加
        if (userForm.isNew() && (userForm.getPassword() == null || userForm.getPassword().isEmpty())) {
            result.rejectValue("password", "", "新規登録時はパスワードが必須です");
            model.addAttribute("roles", userService.findAllRoles());
            return "admin/user_form";
        }

        userService.save(userForm);
        return "redirect:/admin/users";
    }

    // 削除処理
    @PostMapping("/users/{userCode}/delete")
    public String delete(@PathVariable String userCode) {
        // ※ 自分自身(ログイン中のadmin等)を削除できないようにする制御を入れるとより安全です
        userService.delete(userCode);
        return "redirect:/admin/users";
    }
/* 
    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        var u = userService.findById(id);
        if (u.isEmpty()) {
            return "redirect:/admin/users";
        }
        List<UserRole> allRoles = roleRepo.findAll();
        model.addAttribute("user", u.get());
        model.addAttribute("allRoles", allRoles);
        // extract role ids for checking boxes
        Set<Long> assigned = u.get().getRoles().stream().map(UserRole::getId).collect(Collectors.toSet());
        model.addAttribute("assignedRoleIds", assigned);
        return "admin/edit";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id, HttpServletRequest req) {
        var opt = userService.findById(id);
        if (opt.isEmpty()) return "redirect:/admin/users";

        AppUser u = opt.get();
        String[] selected = req.getParameterValues("roles"); // role ids as strings
        Set<UserRole> newRoles = new HashSet<>();
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
    } */
}