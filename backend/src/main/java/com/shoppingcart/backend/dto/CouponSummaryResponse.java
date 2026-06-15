package com.shoppingcart.backend.dto;

import com.shoppingcart.backend.entity.Coupon;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 優惠券摘要回應 DTO（供前端顯示可用優惠券清單，GET /api/coupons/available）。
 * 不包含使用次數等後台敏感資訊。
 */
public class CouponSummaryResponse {

    /** 優惠券代碼 */
    private String code;

    /** 優惠券名稱 */
    private String name;

    /** 優惠券說明 */
    private String description;

    /** 折扣類型：PERCENTAGE 或 FIXED */
    private String discountType;

    /** 折扣值 */
    private BigDecimal discountValue;

    /** 最低訂單金額門檻 */
    private BigDecimal minOrderAmount;

    /** 有效截止日期 */
    private Instant endDate;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 序列化用無參數建構子 */
    public CouponSummaryResponse() {}

    /**
     * 從 Coupon Entity 建立摘要回應。
     *
     * @param coupon 優惠券 Entity
     */
    public static CouponSummaryResponse from(Coupon coupon) {
        CouponSummaryResponse dto = new CouponSummaryResponse();
        dto.code = coupon.getCode();
        dto.name = coupon.getName();
        dto.description = coupon.getDescription();
        dto.discountType = coupon.getDiscountType().name();
        dto.discountValue = coupon.getDiscountValue();
        dto.minOrderAmount = coupon.getMinOrderAmount();
        dto.endDate = coupon.getEndDate();
        return dto;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /** 取得優惠券代碼 */
    public String getCode() { return code; }

    /** 取得優惠券名稱 */
    public String getName() { return name; }

    /** 取得優惠券說明 */
    public String getDescription() { return description; }

    /** 取得折扣類型字串 */
    public String getDiscountType() { return discountType; }

    /** 取得折扣值 */
    public BigDecimal getDiscountValue() { return discountValue; }

    /** 取得最低訂單金額門檻 */
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }

    /** 取得有效截止日期 */
    public Instant getEndDate() { return endDate; }
}
