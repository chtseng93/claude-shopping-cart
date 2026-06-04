package com.shoppingcart.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 購物車 Entity — 對應資料表 cart。
 * 以 sessionId（cookie）識別訪客，無需會員登入。
 * checkedOutAt 為 NULL 表示尚未結帳；有值則表示已完成結帳。
 */
@Entity
@Table(name = "cart")
public class Cart {

    /** 購物車唯一識別碼（UUID），由資料庫自動產生 */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** 訪客 Session 識別碼，不可為空 */
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    /** 購物車建立時間，由 Hibernate 於 INSERT 時自動填入 now() */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 結帳完成時間；NULL 表示尚未結帳 */
    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    /**
     * 購物車所含明細項目。
     * cascade=ALL：對 Cart 的任何持久化操作都連帶套用至 CartItem。
     * orphanRemoval=true：從 items 清單移除的 CartItem 會自動從 DB 刪除。
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** JPA 必要的無參數建構子 */
    protected Cart() {}

    /** 以 sessionId 建立新購物車 */
    public Cart(String sessionId) {
        this.sessionId = sessionId;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public UUID getId() { return id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getCheckedOutAt() { return checkedOutAt; }
    /** 結帳時呼叫此方法，將結帳時間設為 now() */
    public void setCheckedOutAt(Instant checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    public List<CartItem> getItems() { return items; }
}
