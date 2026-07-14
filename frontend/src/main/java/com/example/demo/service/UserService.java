package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.Role;
import com.example.demo.dto.UserForm;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // セレクトボックス用（権限一覧）
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }
    
    // 編集画面表示用：EntityからFormへ詰め替え
    public UserForm getUserForm(String userCode) {
        User user = userRepository.findById(userCode).orElseThrow();
        UserForm form = new UserForm();
        form.setUserCode(user.getUserCode());
        form.setUserName(user.getUserName());
        form.setRoleId(user.getRole().getRoleId());
        form.setNew(false); // 更新モード
        return form;
    }

    // 保存処理（登録・更新兼用）
    @Transactional
    public void save(UserForm form) {
        User user;
        LocalDateTime now = LocalDateTime.now();
        if (form.isNew()) {
            user = new User();
            user.setUserCode(form.getUserCode());
            // Formには持たせていない、システム側の必須情報をセット
            user.setEntryUser(form.getUserCode());
            user.setEntryDatetime(now);
        } else {
            user = userRepository.findById(form.getUserCode()).orElseThrow();
        }

        user.setUserName(form.getUserName());
        // Formには持たせていない、システム側の必須情報をセット
        user.setUpdateUser(form.getUserCode());
        user.setUpdateDatetime(now);
        
        // Roleの紐付け
        Role role = roleRepository.findById(form.getRoleId()).orElseThrow();
        user.setRole(role);

        // パスワードが入力されている場合のみ上書き（暗号化）
        if (form.getPassword() != null && !form.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        userRepository.save(user);
    }

    @Transactional
    public void delete(String userCode) {
        userRepository.deleteById(userCode);
    }
}