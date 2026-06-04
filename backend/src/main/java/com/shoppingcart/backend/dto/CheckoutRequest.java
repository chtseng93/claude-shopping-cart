package com.shoppingcart.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 結帳請求 DTO，包裝 RecipientDto 作為結帳 API（POST /api/cart/checkout）的請求 Body。
 * 使用 @Valid 觸發對 recipient 欄位的巢狀 Bean Validation。
 */
public class CheckoutRequest {

    /** 收件人資料，不可為 null，並觸發巢狀驗證 */
    @NotNull(message = "收件人資料不可為空")
    @Valid
    private RecipientDto recipient;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    public CheckoutRequest() {}

    /**
     * 完整建構子。
     *
     * @param recipient 收件人資料 DTO
     */
    public CheckoutRequest(RecipientDto recipient) {
        this.recipient = recipient;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得收件人資料 */
    public RecipientDto getRecipient() { return recipient; }

    /** 設定收件人資料 */
    public void setRecipient(RecipientDto recipient) { this.recipient = recipient; }
}
