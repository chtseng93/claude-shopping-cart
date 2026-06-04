package com.shoppingcart.backend.repository;

import com.shoppingcart.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 購物車明細資料存取層，繼承 JpaRepository 以取得基本 CRUD 操作。
 * 提供依購物車 ID 查詢明細清單，以及依購物車 ID + 商品 ID 查詢特定明細的方法，
 * 後者用於實現「加入同商品時自動合併數量」的業務邏輯。
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * 查詢指定購物車的所有明細項目，用於計算購物車合計。
     *
     * @param cartId 購物車 UUID
     * @return 該購物車的所有明細清單（可能為空清單）
     */
    List<CartItem> findByCartId(UUID cartId);

    /**
     * 依購物車 ID 與商品 ID 查詢特定明細，用於判斷是否需要合併數量。
     * 若回傳 Optional.empty()，代表該商品尚未加入此購物車，需建立新明細。
     * 若回傳 Optional.of(item)，代表已存在，僅需累加數量。
     *
     * @param cartId    購物車 UUID
     * @param productId 商品 UUID
     * @return 對應的購物車明細（若存在）
     */
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
}
