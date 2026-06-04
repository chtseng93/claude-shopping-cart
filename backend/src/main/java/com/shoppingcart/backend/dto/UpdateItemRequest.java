package com.shoppingcart.backend.dto;

import jakarta.validation.constraints.Min;

/**
 * 更新購物車明細數量請求 DTO，對應 api.md §4.3 的請求格式。
 * quantity = 0 表示移除該明細，quantity > 0 表示更新數量。
 * 使用 Bean Validation 確保數量不為負值。
 */
public class UpdateItemRequest {

    /**
     * 欲更新的數量，允許 0（= 移除明細），不允許負值。
     * 前端亦應阻擋 quantity < 0 的情況，伺服器端作為最後防線。
     */
    @Min(value = 0, message = "數量不可為負數")
    private int quantity;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    public UpdateItemRequest() {}

    /**
     * 完整建構子。
     *
     * @param quantity 更新數量（≥ 0；0 表示移除）
     */
    public UpdateItemRequest(int quantity) {
        this.quantity = quantity;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得更新數量 */
    public int getQuantity() { return quantity; }

    /** 設定更新數量 */
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
