package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "M_ROLE", catalog = "python_schema")
@Data
public class Role {

    @Id
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "auth_entry_flag", nullable = false)
    private boolean authEntryFlag;

    @Column(name = "auth_delete_flag", nullable = false)
    private boolean authDeleteFlag;

    @Column(name = "entry_datetime")
    private LocalDateTime entryDatetime;

    @Column(name = "entry_user")
    private String entryUser;

    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;

    @Column(name = "update_user")
    private String updateUser;
}