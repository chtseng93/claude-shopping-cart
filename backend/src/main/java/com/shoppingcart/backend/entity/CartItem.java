package com.shoppingcart.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 購物車明細 Entity — 對應資料表 cart_item。
 * 記錄某張購物車中加入的商品、數量，以及加入當下的單價快照（unitPrice）。
 * 唯一約束確保同一購物車中每個商品只會出現一列（重複加入時合併數量）。
 */
@Entity
@Table(
    name = "cart_item",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"})
)
public class CartItem {

    /** 明細唯一識別碼（UUID），由資料庫自動產生 */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 所屬購物車（多對一）。
     * 外鍵 cart_id 不可為空。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * 對應商品（多對一）。
     * 外鍵 product_id 不可為空。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 商品數量，不可為空且必須 ≥ 1 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 加入購物車當下的單價快照（TWD）。
     * 快照設計確保商品調價後不影響既有購物車金額。
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** JPA 必要的無參數建構子 */
    protected CartItem() {}

    /** 建立新明細的完整建構子，同步快照當下單價 */
    public CartItem(Cart cart, Product product, Integer quantity, BigDecimal unitPrice) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public UUID getId() { return id; }

    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    /** 更新數量（自動合併或手動調整時呼叫） */
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
