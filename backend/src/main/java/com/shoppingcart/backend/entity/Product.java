package com.shoppingcart.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 商品 Entity — 對應資料表 product。
 * 儲存商品名稱、描述、單價、庫存與圖片等基本資訊。
 */
@Entity
@Table(name = "product")
public class Product {

    /** 商品唯一識別碼（UUID），由資料庫自動產生 */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** 商品名稱，不可為空 */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** 商品描述，允許長文字 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 商品單價（TWD），不可為空且必須 ≥ 0 */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** 庫存數量，不可為空且必須 ≥ 0 */
    @Column(name = "stock", nullable = false)
    private Integer stock;

    /** 商品圖片 URL */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /** 商品建立時間，由 Hibernate 於 INSERT 時自動填入 now() */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** JPA 必要的無參數建構子 */
    protected Product() {}

    /** 建立新商品的完整建構子 */
    public Product(String name, String description, BigDecimal price,
                   Integer stock, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getCreatedAt() { return createdAt; }
}
