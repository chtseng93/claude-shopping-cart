import { apiFetch } from './client'

/**
 * 取得當前訪客的購物車資料（含 total 與 totalQuantity）。
 *
 * 對應 API：GET /api/cart
 * 若訪客無 SESSION_ID cookie，伺服器會自動建立並以 Set-Cookie 回傳。
 *
 * @returns {Promise<Object>} 購物車物件，包含：
 *   - cartId {string}
 *   - items {Array} 每筆含 itemId、productId、name、unitPrice、quantity、subtotal
 *   - totalQuantity {number} 全部品項數量加總（供徽章顯示）
 *   - total {number} 購物車合計金額
 */
export async function getCart() {
  return apiFetch('/api/cart')
}

/**
 * 將商品加入購物車（若已存在相同 productId 則自動合併數量）。
 *
 * 對應 API：POST /api/cart/items
 *
 * @param {string} productId - 要加入的商品 UUID
 * @param {number} [quantity=1] - 加入數量，需 ≥ 1
 * @returns {Promise<Object>} 更新後的完整購物車（結構同 getCart）
 * @throws {Error} productId 不存在（404）或 quantity < 1（400）
 */
export async function addToCart(productId, quantity = 1) {
  return apiFetch('/api/cart/items', {
    method: 'POST',
    body: JSON.stringify({ productId, quantity }),
  })
}

/**
 * 更新購物車明細數量。
 * quantity 為 0 時伺服器會自動移除該明細。
 *
 * 對應 API：PATCH /api/cart/items/{itemId}
 *
 * @param {string} itemId - 購物車明細 UUID
 * @param {number} quantity - 新數量，≥ 0（0 表示刪除）
 * @returns {Promise<Object>} 更新後的完整購物車（結構同 getCart）
 * @throws {Error} itemId 不存在（404）或 quantity < 0（400）
 */
export async function updateCartItem(itemId, quantity) {
  return apiFetch(`/api/cart/items/${itemId}`, {
    method: 'PATCH',
    body: JSON.stringify({ quantity }),
  })
}

/**
 * 直接移除購物車中的指定明細。
 *
 * 對應 API：DELETE /api/cart/items/{itemId}
 *
 * @param {string} itemId - 購物車明細 UUID
 * @returns {Promise<Object>} 更新後的完整購物車（結構同 getCart）
 * @throws {Error} itemId 不存在（404）
 */
export async function removeCartItem(itemId) {
  return apiFetch(`/api/cart/items/${itemId}`, {
    method: 'DELETE',
  })
}

/**
 * 送出結帳，填入收件資料並完成訂單。
 * 可選擇傳入優惠券代碼，伺服器驗證並計算折扣（折扣金額由伺服器決定，非前端計算）。
 *
 * 對應 API：POST /api/cart/checkout
 * 伺服器會驗證庫存、扣減庫存、套用優惠券（若傳入）、記錄收件資料，並將購物車標記為已結帳。
 *
 * @param {{ name: string, phone: string, email: string, address: string }} recipient - 收件人資料
 * @param {string|null} [couponCode=null] - 優惠券代碼（選填，null 表示不使用優惠券）
 * @returns {Promise<Object>} 訂單摘要，包含：
 *   - cartId {string}
 *   - checkedOutAt {string} ISO-8601 結帳時間
 *   - recipient {Object} 收件人資料
 *   - items {Array} 訂單明細
 *   - total {number} 折扣前合計金額
 *   - discountAmount {number} 折扣金額（無優惠券時為 0）
 *   - finalTotal {number} 折扣後實付金額
 *   - couponCode {string|null} 套用的優惠券代碼
 * @throws {Error} 購物車為空、庫存不足或收件資料驗證失敗（400）
 */
export async function checkout(recipient, couponCode = null) {
  return apiFetch('/api/cart/checkout', {
    method: 'POST',
    body: JSON.stringify({ recipient, couponCode }),
  })
}
