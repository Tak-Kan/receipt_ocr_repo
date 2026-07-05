package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import lombok.Data;

@Entity
@Table(name = "T_ACCOUNT", catalog = "python_schema")
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "purchase_datetime")
    private LocalDateTime purchaseDatetime;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "entry_datetime")
    private LocalDateTime entryDatetime;

    @Column(name = "entry_user")
    private String entryUser;

    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;

    @Column(name = "update_user")
    private String updateUser;

    // mappedByは子エンティティ側でヘッダーを保持しているフィールド名
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountDetail> details = new ArrayList<>();

    // 関連付けを補助する便利なメソッド
    public void addDetail(AccountDetail detail) {
        details.add(detail);
        detail.setAccount(this);
    }
}