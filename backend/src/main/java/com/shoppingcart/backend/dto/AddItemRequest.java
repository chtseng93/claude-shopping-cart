package com.shoppingcart.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 加入購物車請求 DTO，對應 api.md §4.2 的請求格式。
 * 使用 Bean Validation 確保商品 ID 不為 null、數量至少為 1。
 */
public class AddItemRequest {

    /** 欲加入的商品 UUID，必填 */
    @NotNull(message = "productId 不可為空")
    private UUID productId;

    /** 加入的數量，必須 ≥ 1 */
    @Min(value = 1, message = "數量必須至少為 1")
    private int quantity;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    public AddItemRequest() {}

    /**
     * 完整建構子。
     *
     * @param productId 商品 ID
     * @param quantity  數量（≥ 1）
     */
    public AddItemRequest(UUID productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得商品 ID */
    public UUID getProductId() { return productId; }

    /** 設定商品 ID */
    public void setProductId(UUID productId) { this.productId = productId; }

    /** 取得數量 */
    public int getQuantity() { return quantity; }

    /** 設定數量 */
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
