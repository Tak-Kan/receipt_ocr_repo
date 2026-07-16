// AccountCategoryForm.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountCategoryForm {

    private Integer categoryId;

    @NotBlank(message = "費目名は必須です")
    private String categoryName;

    private String memo;

    // 新規登録か更新かを判定するフラグ
    private boolean isNew;
}