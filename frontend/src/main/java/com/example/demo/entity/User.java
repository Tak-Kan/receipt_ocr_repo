package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "M_USER", catalog = "python_schema")
@Data
public class User {

    @Id
    @Column(name = "user_code", nullable = false)
    private String userCode;

    @Column(name = "user_name")
    private String userName;

    // 💡 ログイン用のパスワード（暗号化して保存します）
    @Column(name = "password", nullable = false)
    private String password;

    // @Column(name = "role_id")
    // private String roleId;

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

    // 💡 Roleエンティティと role_id で結合します（多対1の関係）
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;
}