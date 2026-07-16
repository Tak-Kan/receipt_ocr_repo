package com.example.demo.mapper;

import com.example.demo.dto.AccountForm;
import com.example.demo.dto.AccountDetailForm;
import com.example.demo.entity.Account;
import com.example.demo.entity.AccountDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// SpringのDIコンテナ(@Autowired)で使えるように componentModel = "spring" を指定
@Mapper(componentModel = "spring")
public interface AccountMapper {

    // Form -> Entityへの変換ルール
    // purchaseDate(String) を LocalDateTime へ指定のフォーマットで自動変換します
    @Mapping(target = "purchaseDatetime", dateFormat = "yyyy-MM-dd'T'HH:mm")
    @Mapping(target = "imagePath", source = "receiptImagePath")
    Account toEntity(AccountForm form);

    @Mapping(target = "entryUser", ignore = true)    // コントローラー/サービスでセットするため無視
    @Mapping(target = "account", ignore = true)  // 双方向リレーションの親設定はサービス層で行う
    AccountDetail toEntity(AccountDetailForm form);

    // Entity -> Formへの変換ルール（明細リストも自動でマッピングされます）
    @Mapping(target = "receiptImagePath", source = "imagePath")
    @Mapping(target = "categoryId", source = "accountCategory.categoryId")
    AccountForm toForm(Account entity);
}