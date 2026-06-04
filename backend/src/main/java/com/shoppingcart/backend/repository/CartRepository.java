package com.shoppingcart.backend.repository;

import com.shoppingcart.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 購物車資料存取層，繼承 JpaRepository 以取得基本 CRUD 操作。
 * 提供依 sessionId 查詢尚未結帳（checkedOutAt 為 null）之活躍購物車的方法。
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * 以 sessionId 查詢尚未結帳的購物車（checkedOutAt 為 null 表示活躍中）。
     * 用於識別訪客的當前購物車，同一 session 同時只有一張活躍購物車。
     *
     * @param sessionId 訪客 cookie 中的 session 識別碼
     * @return 尚未結帳的購物車（若存在）
     */
    Optional<Cart> findBySessionIdAndCheckedOutAtIsNull(String sessionId);
}
