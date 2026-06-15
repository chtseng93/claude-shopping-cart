package com.shoppingcart.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 優惠券使用記錄 Entity — 對應資料表 coupon_usage。
 * 記錄每次優惠券被消耗的詳情（對應哪張訂單、哪個 session、折扣金額）。
 * 訂單取消時刪除此記錄，並返還 Coupon.usageCount。
 */
@Entity
@Table(name = "coupon_usage")
public class CouponUsage {

    /** 使用記錄唯一識別碼（UUID），由資料庫自動產生 */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 對應的優惠券。
     * ManyToOne：多筆使用記錄對應同一張優惠券。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    /** 對應已結帳購物車（訂單）的 UUID */
    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    /** 使用者 session 識別碼（訪客識別） */
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    /** 實際折扣金額（由伺服器計算後儲存，非客戶端傳入） */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** 優惠券使用時間，由 Hibernate 於 INSERT 時自動填入 now() */
    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** JPA 必要的無參數建構子 */
    protected CouponUsage() {}

    /**
     * 建立優惠券使用記錄。
     *
     * @param coupon         使用的優惠券 Entity
     * @param cartId         對應已結帳購物車 ID（訂單）
     * @param sessionId      使用者 session 識別碼
     * @param discountAmount 伺服器計算的實際折扣金額
     */
    public CouponUsage(Coupon coupon, UUID cartId, String sessionId, BigDecimal discountAmount) {
        this.coupon = coupon;
        this.cartId = cartId;
        this.sessionId = sessionId;
        this.discountAmount = discountAmount;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /** 取得使用記錄 ID */
    public UUID getId() { return id; }

    /** 取得對應的優惠券 */
    public Coupon getCoupon() { return coupon; }

    /** 取得對應的購物車（訂單）ID */
    public UUID getCartId() { return cartId; }

    /** 取得使用者 session 識別碼 */
    public String getSessionId() { return sessionId; }

    /** 取得實際折扣金額 */
    public BigDecimal getDiscountAmount() { return discountAmount; }

    /** 取得使用時間 */
    public Instant getUsedAt() { return usedAt; }
}
