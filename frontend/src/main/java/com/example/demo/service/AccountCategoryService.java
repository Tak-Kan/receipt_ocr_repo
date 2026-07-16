// AccountCategoryService.java
package com.example.demo.service;

import com.example.demo.entity.AccountCategory;
import com.example.demo.dto.AccountCategoryForm;
import com.example.demo.repository.AcCategoryRepository;
import com.example.demo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountCategoryService {

    private final AcCategoryRepository acCategoryRepository;
    private final AccountRepository accountRepository;

    public List<AccountCategory> findAllAccountCategorys() {
        return acCategoryRepository.findAll();
    }

    public AccountCategoryForm getAccountCategoryForm(Integer categoryId) {
        AccountCategory accountCategory = acCategoryRepository.findById(categoryId).orElseThrow();
        AccountCategoryForm form = new AccountCategoryForm();
        form.setCategoryId(accountCategory.getCategoryId());
        form.setCategoryName(accountCategory.getCategoryName());
        form.setMemo(accountCategory.getMemo());
        form.setNew(false);
        return form;
    }

    @Transactional
    public void save(AccountCategoryForm form, String userName) {
        LocalDateTime now = LocalDateTime.now();
        AccountCategory accountCategory;
        if (form.isNew()) {
            accountCategory = new AccountCategory();
            // 自動採番ではないため、現在の最大ID + 1 を設定する簡易的な処理
            int maxId = acCategoryRepository.findAll().stream()
                    .mapToInt(AccountCategory::getCategoryId)
                    .max().orElse(0);
            accountCategory.setCategoryId(maxId + 1);
        } else {
            accountCategory = acCategoryRepository.findById(form.getCategoryId()).orElseThrow();
        }

        accountCategory.setCategoryName(form.getCategoryName());
        accountCategory.setMemo(form.getMemo());
        // Formには持たせていない、システム側の必須情報をセット
        accountCategory.setEntryUser(userName);
        accountCategory.setEntryDatetime(now);

        acCategoryRepository.save(accountCategory);
    }

    @Transactional
    public void delete(Integer categoryId) {
        // 使用中のカテゴリは削除不可
        if (accountRepository.existsByAccountCategory_CategoryId(categoryId)) {
            throw new IllegalStateException("このカテゴリは既に家計簿で使用されているため削除できません。");
        }
        acCategoryRepository.deleteById(categoryId);
    }
}