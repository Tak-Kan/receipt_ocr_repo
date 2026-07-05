package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.Set;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "T_ACCOUNT_DETAIL", catalog = "python_schema")
@Data
public class AccountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_number")
    private Long detailNumber;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "item_amount")
    private Integer itemAmount;

    @Column(name = "entry_datetime")
    private LocalDateTime entryDatetime;

    @Column(name = "entry_user")
    private String entryUser;

    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;

    @Column(name = "update_user")
    private String updateUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}