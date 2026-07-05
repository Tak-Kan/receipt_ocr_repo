package com.example.demo.service;

import com.example.demo.dto.AccountForm;
import com.example.demo.entity.Account;
import com.example.demo.entity.AccountDetail;
import com.example.demo.mapper.AccountMapper;
import com.example.demo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMapper accountMapper; // MapStructの自動生成クラスをDI

    @Transactional
    public void saveAccount(AccountForm form, String userName) {
        // 1. MapStructを使用して、Form(DTO)からEntityへ一発で変換
        // （子要素のList<AccountDetailForm>も自動で List<AccountDetail> に変換されます）
        Account account = accountMapper.toEntity(form);

        LocalDateTime now = LocalDateTime.now();
        
        // 2. Formには持たせていない、システム側の必須情報をセット
        account.setEntryUser(userName);
        account.setEntryDatetime(now);

        // 3. JPAの双方向リレーションシップを解決するため、子から親への参照をセット
        if (account.getDetails() != null) {
            for (AccountDetail detail : account.getDetails()) {
                detail.setAccount(account);
                detail.setEntryUser(userName);
                detail.setEntryDatetime(now);
            }
        }

        // 4. 保存 (CascadeType.ALLにより子も同時に保存される)
        accountRepository.save(account);
    }
}