package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class AccountForm {

    private Long accountId; // 新規追加（新規登録時はnull、編集時は値が入る）

    @NotBlank(message = "購入店舗は必須です")
    @Size(max = 255, message = "購入店舗は255文字以内で入力してください")
    private String storeName;

    @NotNull(message = "購入日時は必須です")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime purchaseDatetime;

    @NotNull(message = "合計金額は必須です")
    @Min(value = 0, message = "合計金額は0以上の数値を入力してください")
    private Integer totalAmount;

    private String receiptImagePath; // 任意項目のためアノテーションなし

    @Valid // ネストされた子要素（リスト）のバリデーションを有効にする魔法のアノテーション
    @NotEmpty(message = "購入品は最低1件以上必要です")
    private List<AccountDetailForm> details = new ArrayList<>();
}