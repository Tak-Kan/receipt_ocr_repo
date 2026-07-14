// UserDetailsServiceImpl.java
package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository2;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository2 userRepository;

    /**
     * Spring Securityがログイン処理を行う際に自動的に呼び出されるメソッド
     * @param username ログイン画面で入力されたID（今回は userCode が渡ってきます）
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ① ここが呼ばれているか、入力値が渡ってきているか確認
        System.out.println("★★★ ログイン処理開始: 入力されたユーザーコード = " + username);
        // 1. ログイン画面で入力された「userCode（6桁）」を使ってDBを検索
        // Userエンティティの主キーが userCode なので、findById で検索可能です
        User user = userRepository.findById(username)
                .orElseThrow(() -> {
                    // ② DB検索に失敗した場合
                    System.out.println("★★★ エラー: DBにユーザーが見つかりません！");
                    return new UsernameNotFoundException("ユーザーが見つかりません");
                });

        // ③ DB検索に成功した場合
        System.out.println("★★★ DB検索成功: 取得したパスワード(暗号化済) = " + user.getPassword());
        // 2. 見つかったUserエンティティを、先ほど作った翻訳用クラスに詰めてSpring Securityへ返す
        return new LoginUserDetails(user);
    }
}