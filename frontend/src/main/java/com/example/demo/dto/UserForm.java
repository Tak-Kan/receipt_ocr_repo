// UserForm.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserForm {

    @NotBlank(message = "ユーザーコードは必須です")
    @Size(min = 6, max = 6, message = "ユーザーコードは6桁で入力してください")
    private String userCode;

    @NotBlank(message = "ユーザー名は必須です")
    private String userName;

    // 新規作成時は必須、更新時は未入力なら変更なしとするため @NotBlank は付けません
    private String password;


    @NotNull(message = "権限を選択してください")
    private Integer roleId;

    // 新規登録か更新かを判定するためのフラグ
    private boolean isNew;
}