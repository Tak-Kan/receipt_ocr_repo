// src/main/java/com/example/demo/entity/Category.java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "M_ACCOUNT_CATEGORY")
@Data
public class AccountCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "memo")
    private String memo;

    @Column(name = "entry_datetime")
    private LocalDateTime entryDatetime;

    @Column(name = "entry_user")
    private String entryUser;

    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;

    @Column(name = "update_user")
    private String updateUser;
}