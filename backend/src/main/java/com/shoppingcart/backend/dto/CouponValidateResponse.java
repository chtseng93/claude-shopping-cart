package com.shoppingcart.backend.dto;

import com.shoppingcart.backend.entity.Coupon;

import java.math.BigDecimal;

/**
 * 優惠券驗證試算回應 DTO。
 * 包含折扣計算結果（discountAmount、finalAmount），均由伺服器計算，前端不得自行計算。
 */
public class CouponValidateResponse {

    /** 優惠券代碼 */
    private String code;

    /** 優惠券名稱（顯示用） */
    private String name;

    /** 優惠券說明文字 */
    private String description;

    /** 折扣類型：PERCENTAGE 或 FIXED */
    private String discountType;

    /** 折扣值（百分比數字 或 TWD 金額） */
    private BigDecimal discountValue;

    /** 折扣金額（由伺服器計算） */
    private BigDecimal discountAmount;

    /** 原始訂單金額（等同請求的 orderAmount） */
    private BigDecimal originalAmount;

    /** 折扣後實付金額（= originalAmount - discountAmount） */
    private BigDecimal finalAmount;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 序列化用無參數建構子 */
    public CouponValidateResponse() {}

    /**
     * 從 Coupon Entity 與計算結果建立回應 DTO。
     *
     * @param coupon         優惠券 Entity
     * @param discountAmount 伺服器計算的折扣金額
     * @param originalAmount 原始訂單金額
     */
    public CouponValidateResponse(Coupon coupon, BigDecimal discountAmount, BigDecimal originalAmount) {
        this.code = coupon.getCode();
        this.name = coupon.getName();
        this.description = coupon.getDescription();
        this.discountType = coupon.getDiscountType().name();
        this.discountValue = coupon.getDiscountValue();
        this.discountAmount = discountAmount;
        this.originalAmount = originalAmount;
        // 折扣後金額不小於 0
        this.finalAmount = originalAmount.subtract(discountAmount).max(BigDecimal.ZERO);
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

    /** 取得伺服器計算的折扣金額 */
    public BigDecimal getDiscountAmount() { return discountAmount; }

    /** 取得原始訂單金額 */
    public BigDecimal getOriginalAmount() { return originalAmount; }

    /** 取得折扣後實付金額 */
    public BigDecimal getFinalAmount() { return finalAmount; }
}
