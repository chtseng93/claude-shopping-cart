package com.shoppingcart.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 結帳成功後的訂單摘要 DTO，對應 api.md §5.1 的回應格式。
 * 包含結帳購物車 ID、結帳時間、收件資料、明細清單及合計金額。
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

    /** 訂單合計金額，由伺服器計算 */
    private BigDecimal total;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /**
     * 完整建構子。
     *
     * @param cartId        購物車 ID
     * @param checkedOutAt  結帳時間
     * @param recipient     收件人資料
     * @param items         明細 DTO 清單
     * @param total         合計金額（伺服器計算）
     */
    public OrderSummary(UUID cartId, Instant checkedOutAt, RecipientDto recipient,
                        List<ItemDto> items, BigDecimal total) {
        this.cartId = cartId;
        this.checkedOutAt = checkedOutAt;
        this.recipient = recipient;
        this.items = items;
        this.total = total;
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

    /** 取得合計金額 */
    public BigDecimal getTotal() { return total; }
}
