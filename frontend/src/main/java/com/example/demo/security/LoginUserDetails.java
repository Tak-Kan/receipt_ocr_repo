// LoginUserDetails.java
package com.example.demo.security;

import com.example.demo.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LoginUserDetails implements UserDetails {

    // データベースから取得した実際のUserエンティティを保持
    private final User user;

    public LoginUserDetails(User user) {
        this.user = user;
    }

    // Thymeleaf（画面）などから、ログイン中のユーザー情報にアクセスするためのメソッド
    public User getUser() {
        return user;
    }

    /**
     * 💡 ここが超重要！
     * DBのRole情報とフラグを、Spring Securityが理解できる「権限」に変換して渡します。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // RoleIdに基づいてベースの権限を付与 (1: 管理者, それ以外: 一般)
        if (user.getRole().getRoleId() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // DBのフラグに基づいて、細かい操作権限を付与
        if (user.getRole().isAuthEntryFlag()) {
            authorities.add(new SimpleGrantedAuthority("AUTH_ENTRY")); // 登録権限
        }
        if (user.getRole().isAuthDeleteFlag()) {
            authorities.add(new SimpleGrantedAuthority("AUTH_DELETE")); // 削除権限
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // 💡 Spring Securityの仕様上メソッド名は"getUsername"ですが、
        // 今回は6桁の「ユーザーコード(userCode)」をログインIDとして使用します。
        return user.getUserCode();
    }

    // --- 以下はアカウントの有効期限やロック状態を管理する設定（今回はすべてtrueで有効化） ---
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}