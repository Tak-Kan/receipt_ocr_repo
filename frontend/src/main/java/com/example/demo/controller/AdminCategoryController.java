// AdminAccountCategoryController.java
package com.example.demo.controller;

import com.example.demo.dto.AccountCategoryForm;
import com.example.demo.service.AccountCategoryService;
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
@RequestMapping("/admin/accountCategorys")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AccountCategoryService accountCategoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accountCategorys", accountCategoryService.findAllAccountCategorys());
        return "admin/category_list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AccountCategoryForm form = new AccountCategoryForm();
        form.setNew(true);
        model.addAttribute("accountCategoryForm", form);
        return "admin/category_form";
    }

    @GetMapping("/{categoryId}/edit")
    public String editForm(@PathVariable Integer categoryId, Model model) {
        model.addAttribute("accountCategoryForm", accountCategoryService.getAccountCategoryForm(categoryId));
        return "admin/category_form";
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute AccountCategoryForm accountCategoryForm, Authentication auth, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/category_form";
        }
        
        String userName = (auth != null) ? auth.getName() : "anonymous";

        try {
            accountCategoryService.save(accountCategoryForm, userName);
        } catch (IllegalArgumentException e) {
            result.rejectValue("authEntryFlag", "", e.getMessage());
            return "admin/category_form";
        }

        return "redirect:/admin/accountCategorys";
    }

    @PostMapping("/{categoryId}/delete")
    public String delete(@PathVariable Integer categoryId, RedirectAttributes redirectAttributes) {
        try {
            accountCategoryService.delete(categoryId);
        } catch (Exception e) {
            // 削除エラー時（使用中など）は一覧画面にエラーメッセージを表示してリダイレクト
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/accountCategorys";
    }
}