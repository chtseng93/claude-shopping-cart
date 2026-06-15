package com.shoppingcart.backend.dto;

import com.shoppingcart.backend.entity.CartItem;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 購物車明細項目 DTO。
 * 將 CartItem Entity 轉換為 API 回應格式，
 * 並由伺服器計算 subtotal（unitPrice × quantity），不接受客戶端傳入金額。
 */
public class ItemDto {

    /** 購物車明細的唯一識別碼 */
    private UUID itemId;

    /** 對應商品的唯一識別碼 */
    private UUID productId;

    /** 商品名稱（快照自加入當下） */
    private String name;

    /** 加入購物車當下快照的單價（TWD） */
    private BigDecimal unitPrice;

    /** 商品數量 */
    private int quantity;

    /** 小計金額 = unitPrice × quantity，由伺服器計算 */
    private BigDecimal subtotal;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    protected ItemDto() {}

    /**
     * 完整建構子，供靜態工廠方法使用。
     *
     * @param itemId    購物車明細 ID
     * @param productId 商品 ID
     * @param name      商品名稱
     * @param unitPrice 快照單價
     * @param quantity  數量
     * @param subtotal  小計（伺服器計算）
     */
    public ItemDto(UUID itemId, UUID productId, String name,
                   BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
        this.itemId = itemId;
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    // ── 靜態工廠方法 ──────────────────────────────────────────────────────

    /**
     * 從 CartItem Entity 建立 ItemDto，subtotal 由伺服器計算（unitPrice × quantity）。
     *
     * @param item 購物車明細 Entity
     * @return 對應的 ItemDto，含伺服器計算的小計
     */
    public static ItemDto from(CartItem item) {
        BigDecimal subtotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new ItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                subtotal
        );
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /** 取得購物車明細 ID */
    public UUID getItemId() { return itemId; }

    /** 取得商品 ID */
    public UUID getProductId() { return productId; }

    /** 取得商品名稱 */
    public String getName() { return name; }

    /** 取得快照單價 */
    public BigDecimal getUnitPrice() { return unitPrice; }

    /** 取得商品數量 */
    public int getQuantity() { return quantity; }

    /** 取得伺服器計算的小計金額 */
    public BigDecimal getSubtotal() { return subtotal; }
}
