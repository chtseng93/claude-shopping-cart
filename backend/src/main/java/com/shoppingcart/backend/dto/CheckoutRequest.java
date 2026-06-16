package com.shoppingcart.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 結帳請求 DTO，包裝 RecipientDto 作為結帳 API（POST /api/cart/checkout）的請求 Body。
 * 使用 @Valid 觸發對 recipient 欄位的巢狀 Bean Validation。
 * couponCode 為選填欄位，傳入時由伺服器驗證並套用折扣。
 */
public class CheckoutRequest {

    /** 收件人資料，不可為 null，並觸發巢狀驗證 */
    @NotNull(message = "收件人資料不可為空")
    @Valid
    private RecipientDto recipient;

    /**
     * 優惠券代碼（選填）。
     * 傳入時伺服器自動驗證並計算折扣，不傳入則視為不使用優惠券。
     * 折扣金額由伺服器計算，不接受客戶端傳入折扣金額。
     */
    private String couponCode;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    public CheckoutRequest() {}

    /**
     * 完整建構子。
     *
     * @param recipient  收件人資料 DTO
     * @param couponCode 優惠券代碼（可為 null）
     */
    public CheckoutRequest(RecipientDto recipient, String couponCode) {
        this.recipient = recipient;
        this.couponCode = couponCode;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得收件人資料 */
    public RecipientDto getRecipient() { return recipient; }

    /** 設定收件人資料 */
    public void setRecipient(RecipientDto recipient) { this.recipient = recipient; }

    /** 取得優惠券代碼（可為 null） */
    public String getCouponCode() { return couponCode; }

    /** 設定優惠券代碼 */
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
