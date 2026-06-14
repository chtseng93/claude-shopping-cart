package com.shoppingcart.backend.repository;

import com.shoppingcart.backend.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 優惠券使用記錄資料存取介面。
 * Spring Data JPA 自動實作，提供使用記錄的增刪查操作。
 */
@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    /**
     * 依購物車（訂單）ID 查詢對應的優惠券使用記錄。
     * 一張訂單只能使用一張優惠券，故回傳 Optional。
     *
     * @param cartId 購物車（訂單）UUID
     * @return 優惠券使用記錄 Optional
     */
    Optional<CouponUsage> findByCartId(UUID cartId);
}
