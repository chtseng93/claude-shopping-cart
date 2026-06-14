package com.shoppingcart.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 優惠券驗證試算請求 DTO（POST /api/coupons/validate）。
 * 前端送入優惠券代碼與訂單金額，由伺服器計算折扣後回傳，不接受客戶端傳入折扣金額。
 */
public class CouponValidateRequest {

    /** 優惠券代碼，不可為空；系統自動轉大寫處理 */
    @NotBlank(message = "優惠券代碼不可為空")
    private String code;

    /** 訂單原始金額，必須大於 0；由伺服器計算折扣所需 */
    @NotNull(message = "訂單金額不可為空")
    @DecimalMin(value = "0.01", message = "訂單金額必須大於 0")
    private BigDecimal orderAmount;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    public CouponValidateRequest() {}

    /**
     * 完整建構子（測試用）。
     *
     * @param code        優惠券代碼
     * @param orderAmount 訂單原始金額
     */
    public CouponValidateRequest(String code, BigDecimal orderAmount) {
        this.code = code;
        this.orderAmount = orderAmount;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得優惠券代碼 */
    public String getCode() { return code; }

    /** 設定優惠券代碼 */
    public void setCode(String code) { this.code = code; }

    /** 取得訂單金額 */
    public BigDecimal getOrderAmount() { return orderAmount; }

    /** 設定訂單金額 */
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
}
