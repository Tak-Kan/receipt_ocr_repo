package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_ACCOUNT")
public class TAccount {

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

    @Column(name = "entry_datetime")
    private LocalDateTime entryDatetime;

    @Column(name = "entry_user")
    private String entryUser;

    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;

    @Column(name = "update_user")
    private String updateUser;

    // getters / setters
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public LocalDateTime getPurchaseDatetime() { return purchaseDatetime; }
    public void setPurchaseDatetime(LocalDateTime purchaseDatetime) { this.purchaseDatetime = purchaseDatetime; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getEntryDatetime() { return entryDatetime; }
    public void setEntryDatetime(LocalDateTime entryDatetime) { this.entryDatetime = entryDatetime; }
    public String getEntryUser() { return entryUser; }
    public void setEntryUser(String entryUser) { this.entryUser = entryUser; }
    public LocalDateTime getUpdateDatetime() { return updateDatetime; }
    public void setUpdateDatetime(LocalDateTime updateDatetime) { this.updateDatetime = updateDatetime; }
    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
}