package com.shoppingcart.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 結帳成功後的訂單摘要 DTO，對應 api.md §8.1 的回應格式。
 * 包含結帳購物車 ID、結帳時間、收件資料、明細清單、合計金額及優惠券折扣資訊。
 * 折扣相關金額（discountAmount、finalTotal）均由伺服器計算，不接受客戶端傳入。
 */
public class OrderSummary {

    /** 已結帳的購物車唯一識別碼 */
    private UUID cartId;

    /** 結帳完成時間（UTC ISO-8601 格式） */
    private Instant checkedOutAt;

    /** 收件人資料（姓名、電話、Email、地址） */
    private RecipientDto recipient;

    /** 結帳的購物車明細清單（含快照單價與伺服器計算小計） */
    private List<ItemDto> items;

    /** 折扣前訂單合計金額，由伺服器計算 */
    private BigDecimal total;

    /** 折扣金額，由伺服器依優惠券規則計算（無優惠券時為 0） */
    private BigDecimal discountAmount;

    /** 折扣後實付金額（= total - discountAmount），由伺服器計算 */
    private BigDecimal finalTotal;

    /** 套用的優惠券代碼（無優惠券時為 null） */
    private String couponCode;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    protected OrderSummary() {}

    /**
     * 不含優惠券的建構子（向後相容）。
     *
     * @param cartId        購物車 ID
     * @param checkedOutAt  結帳時間
     * @param recipient     收件人資料
     * @param items         明細 DTO 清單
     * @param total         合計金額（折扣前，伺服器計算）
     */
    public OrderSummary(UUID cartId, Instant checkedOutAt, RecipientDto recipient,
                        List<ItemDto> items, BigDecimal total) {
        this.cartId = cartId;
        this.checkedOutAt = checkedOutAt;
        this.recipient = recipient;
        this.items = items;
        this.total = total;
        this.discountAmount = BigDecimal.ZERO;
        this.finalTotal = total;
        this.couponCode = null;
    }

    /**
     * 含優惠券的完整建構子。
     *
     * @param cartId         購物車 ID
     * @param checkedOutAt   結帳時間
     * @param recipient      收件人資料
     * @param items          明細 DTO 清單
     * @param total          折扣前合計金額（伺服器計算）
     * @param discountAmount 折扣金額（伺服器計算）
     * @param finalTotal     折扣後實付金額（伺服器計算）
     * @param couponCode     套用的優惠券代碼（可為 null）
     */
    public OrderSummary(UUID cartId, Instant checkedOutAt, RecipientDto recipient,
                        List<ItemDto> items, BigDecimal total, BigDecimal discountAmount,
                        BigDecimal finalTotal, String couponCode) {
        this.cartId = cartId;
        this.checkedOutAt = checkedOutAt;
        this.recipient = recipient;
        this.items = items;
        this.total = total;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
        this.couponCode = couponCode;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /** 取得購物車 ID */
    public UUID getCartId() { return cartId; }

    /** 取得結帳時間 */
    public Instant getCheckedOutAt() { return checkedOutAt; }

    /** 取得收件人資料 */
    public RecipientDto getRecipient() { return recipient; }

    /** 取得明細清單 */
    public List<ItemDto> getItems() { return items; }

    /** 取得折扣前合計金額 */
    public BigDecimal getTotal() { return total; }

    /** 取得折扣金額（無優惠券時為 0） */
    public BigDecimal getDiscountAmount() { return discountAmount; }

    /** 取得折扣後實付金額 */
    public BigDecimal getFinalTotal() { return finalTotal; }

    /** 取得套用的優惠券代碼（無優惠券時為 null） */
    public String getCouponCode() { return couponCode; }
}
