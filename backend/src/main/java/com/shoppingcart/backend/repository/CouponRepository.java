package com.shoppingcart.backend.repository;

import com.shoppingcart.backend.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 優惠券資料存取介面。
 * Spring Data JPA 自動實作，提供優惠券主檔的 CRUD 操作。
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    /**
     * 依優惠券代碼查詢（大小寫不敏感）。
     * 系統統一以大寫存入，查詢前需先轉大寫。
     *
     * @param code 優惠券代碼（應已轉大寫）
     * @return 優惠券 Optional，不存在時為 empty
     */
    Optional<Coupon> findByCode(String code);

    /**
     * 查詢目前可用的優惠券清單：
     * - 啟用中（isActive = true）
     * - 在有效期限內（startDate <= now <= endDate）
     * - 未超過使用次數上限（usageCount < maxUsageCount 或 maxUsageCount 為 null）
     *
     * @param now 當前時間（用於有效期限比較）
     * @return 可用的優惠券清單
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.isActive = true
          AND c.startDate <= :now
          AND c.endDate >= :now
          AND (c.maxUsageCount IS NULL OR c.usageCount < c.maxUsageCount)
        ORDER BY c.createdAt DESC
        """)
    List<Coupon> findAvailableCoupons(@Param("now") Instant now);
}
