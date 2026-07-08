package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountDetailForm {

    private Long detailNumber; // 新規追加（新規登録時はnull、編集時は値が入る）

    @NotBlank(message = "商品名は必須です")
    private String itemName;

    @NotNull(message = "金額は必須です")
    @Min(value = 0, message = "金額は0以上を入力してください")
    private Integer itemAmount;
}