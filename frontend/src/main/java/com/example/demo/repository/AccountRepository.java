package com.example.demo.repository;

import com.example.demo.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * ログイン中ユーザーの家計簿情報をすべて取得します（購入日時の新しい順）
     * ※Spring Data JPAの命名規則により、メソッド名だけでクエリが自動生成されます。
     *
     * @param userId ユーザーID
     * @return 家計簿情報のリスト
     */
    List<Account> findByEntryUserOrderByPurchaseDatetimeDesc(String userId);

    /**
     * 検索条件（店舗名、購入日時From～To）に基づいて家計簿情報を動的に検索します。
     * * 【クエリのポイント】
     * - `t.userId = :userId` : 他人のデータが混ざらないよう、ログイン中のユーザーIDで必ず絞り込みます。
     * - `:storeName IS NULL OR :storeName = '' OR ...` : 画面で店舗名が未入力（nullまたは空文字）の場合は、この条件を無視（スルー）します。入力されている場合のみ部分一致（LIKE）で検索します。
     * - 日付（From/To）も同様に、null（未入力）の場合は条件を無視し、入力されている場合のみ範囲指定を行います。
     *
     * @param userId     ユーザーID（必須）
     * @param storeName  購入店舗名（任意・部分一致）
     * @param dateFrom   購入日時From（任意）
     * @param dateTo     購入日時To（任意）
     * @return 検索条件に合致した家計簿情報のリスト
     */
    @Query("SELECT t FROM Account t WHERE " +
           "t.entryUser = :userId AND " +
           "(:keyword IS NULL OR :keyword = '' OR t.storeName LIKE %:keyword%) AND " +
           "(:startDate IS NULL OR t.purchaseDatetime >= :startDate) AND " +
           "(:endDate IS NULL OR t.purchaseDatetime <= :endDate) " +
           "ORDER BY t.purchaseDatetime DESC")
    List<Account> searchAccounts(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword
    );
}