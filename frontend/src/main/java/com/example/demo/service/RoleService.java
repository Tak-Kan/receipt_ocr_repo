// RoleService.java
package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.dto.RoleForm;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    public RoleForm getRoleForm(Integer roleId) {
        Role role = roleRepository.findById(roleId).orElseThrow();
        RoleForm form = new RoleForm();
        form.setRoleId(role.getRoleId());
        form.setRoleName(role.getRoleName());
        form.setAuthEntryFlag(role.isAuthEntryFlag());
        form.setAuthDeleteFlag(role.isAuthDeleteFlag());
        form.setNew(false);
        return form;
    }

    @Transactional
    public void save(RoleForm form, String userName) {
        // 💡 ① スーパー管理者(ID:1)の権限剥奪を防止
        if (!form.isNew() && form.getRoleId() == 1) {
            if (!form.isAuthEntryFlag() || !form.isAuthDeleteFlag()) {
                throw new IllegalArgumentException("システム管理者(ID:1)の権限は外すことができません。");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Role role;
        if (form.isNew()) {
            role = new Role();
            // 自動採番ではないため、現在の最大ID + 1 を設定する簡易的な処理
            int maxId = roleRepository.findAll().stream()
                    .mapToInt(Role::getRoleId)
                    .max().orElse(0);
            role.setRoleId(maxId + 1);
        } else {
            role = roleRepository.findById(form.getRoleId()).orElseThrow();
        }

        role.setRoleName(form.getRoleName());
        role.setAuthEntryFlag(form.isAuthEntryFlag());
        role.setAuthDeleteFlag(form.isAuthDeleteFlag());
        // Formには持たせていない、システム側の必須情報をセット
        role.setEntryUser(userName);
        role.setEntryDatetime(now);

        roleRepository.save(role);
    }

    @Transactional
    public void delete(Integer roleId) {
        // 💡 ② スーパー管理者(ID:1)の削除を防止
        if (roleId == 1) {
            throw new IllegalArgumentException("システム管理者(ID:1)は削除できません。");
        }

        // 💡 ③ 使用中のロールの削除を防止
        if (userRepository.existsByRole_RoleId(roleId)) {
            throw new IllegalStateException("このロールはユーザーに紐づいているため削除できません。先にユーザーのロールを変更してください。");
        }

        roleRepository.deleteById(roleId);
    }
}