package com.shoppingcart.backend.dto;

import com.shoppingcart.backend.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 商品查詢回應 DTO，對應 api.md §3 的回應格式。
 * 透過靜態工廠方法 {@link #from(Product)} 由 Entity 轉換而來。
 */
public class ProductResponse {

    /** 商品唯一識別碼 */
    private UUID id;

    /** 商品名稱 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品單價（TWD） */
    private BigDecimal price;

    /** 目前庫存數量 */
    private Integer stock;

    /** 商品圖片 URL */
    private String imageUrl;

    /** 商品建立時間（UTC ISO-8601） */
    private Instant createdAt;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化所需的無參數建構子 */
    protected ProductResponse() {}

    /** 全欄位建構子，供靜態工廠方法使用 */
    private ProductResponse(UUID id, String name, String description,
                            BigDecimal price, Integer stock,
                            String imageUrl, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    // ── 靜態工廠方法 ────────────────────────────────────────────────────

    /**
     * 將 {@link Product} Entity 轉換為回應 DTO。
     *
     * @param product 來源商品 Entity
     * @return 轉換後的 ProductResponse
     */
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getCreatedAt()
        );
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /** 取得商品 ID */
    public UUID getId() { return id; }

    /** 取得商品名稱 */
    public String getName() { return name; }

    /** 取得商品描述 */
    public String getDescription() { return description; }

    /** 取得商品單價 */
    public BigDecimal getPrice() { return price; }

    /** 取得庫存數量 */
    public Integer getStock() { return stock; }

    /** 取得圖片 URL */
    public String getImageUrl() { return imageUrl; }

    /** 取得建立時間 */
    public Instant getCreatedAt() { return createdAt; }
}
