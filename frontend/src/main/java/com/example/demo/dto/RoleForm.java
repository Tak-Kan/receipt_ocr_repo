// RoleForm.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleForm {

    private Integer roleId;

    @NotBlank(message = "ロール名は必須です")
    private String roleName;

    private boolean authEntryFlag;
    
    private boolean authDeleteFlag;

    // 新規登録か更新かを判定するフラグ
    private boolean isNew;
}