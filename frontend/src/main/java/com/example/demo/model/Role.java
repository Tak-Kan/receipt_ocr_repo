package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "role", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoleName name;

    @ManyToMany(mappedBy = "roles")
    private Set<AppUser> users;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RoleName getName() { return name; }
    public void setName(RoleName name) { this.name = name; }

    public Set<AppUser> getUsers() { return users; }
    public void setUsers(Set<AppUser> users) { this.users = users; }
}