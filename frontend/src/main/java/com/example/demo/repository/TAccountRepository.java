package com.example.demo.repository;

import com.example.demo.model.TAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TAccountRepository extends JpaRepository<TAccount, Long> {
    
    // 画面3用: 店舗名（部分一致）と日付（完全一致）で検索するクエリ
    // 条件が未入力（nullや空文字）の場合は全件マッチするように考慮
    @Query("SELECT r FROM TAccount r WHERE " +
           "(:storeName IS NULL OR r.storeName LIKE %:storeName%) AND " +
           "(:datetime IS NULL OR r.purchaseDatetime = :datetime)")
    List<TAccount> searchTAccounts(
        @Param("storeName") String storeName,
        @Param("datetime") LocalDateTime datetime
        );
}