package com.shoppingcart.backend.dto;

import com.shoppingcart.backend.entity.Cart;
import com.shoppingcart.backend.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 購物車回應 DTO，對應 api.md §4.1 的回應格式。
 * total 與 totalQuantity 一律由伺服器計算，不接受客戶端傳入。
 */
public class CartResponse {

    /** 購物車唯一識別碼 */
    private UUID cartId;

    /** 購物車明細清單 */
    private List<ItemDto> items;

    /** 所有明細數量加總，供前端購物車徽章顯示 */
    private int totalQuantity;

    /** 購物車合計金額，由伺服器以 BigDecimal 累加計算 */
    private BigDecimal total;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /**
     * 完整建構子，供靜態工廠方法使用。
     *
     * @param cartId        購物車 ID
     * @param items         明細 DTO 清單
     * @param totalQuantity 所有明細數量加總
     * @param total         合計金額（伺服器計算）
     */
    public CartResponse(UUID cartId, List<ItemDto> items, int totalQuantity, BigDecimal total) {
        this.cartId = cartId;
        this.items = items;
        this.totalQuantity = totalQuantity;
        this.total = total;
    }

    // ── 靜態工廠方法 ──────────────────────────────────────────────────────

    /**
     * 從 Cart Entity 與 CartItem 清單建立 CartResponse。
     * total 與 totalQuantity 均由此方法計算，不接受外部傳入金額。
     *
     * @param cart  購物車 Entity
     * @param items 購物車明細 Entity 清單
     * @return 含伺服器計算合計的 CartResponse
     */
    public static CartResponse from(Cart cart, List<CartItem> items) {
        // 將每個 CartItem 轉為 ItemDto，小計由 ItemDto.from() 計算
        List<ItemDto> itemDtos = items.stream()
                .map(ItemDto::from)
                .toList();

        // 合計金額：累加各明細小計（伺服器權威，以 BigDecimal.add 確保精度）
        BigDecimal total = itemDtos.stream()
                .map(ItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 總數量：累加各明細數量，供前端徽章顯示
        int totalQuantity = itemDtos.stream()
                .mapToInt(ItemDto::getQuantity)
                .sum();

        return new CartResponse(cart.getId(), itemDtos, totalQuantity, total);
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /** 取得購物車 ID */
    public UUID getCartId() { return cartId; }

    /** 取得購物車明細清單 */
    public List<ItemDto> getItems() { return items; }

    /** 取得所有明細數量加總 */
    public int getTotalQuantity() { return totalQuantity; }

    /** 取得伺服器計算的合計金額 */
    public BigDecimal getTotal() { return total; }
}
