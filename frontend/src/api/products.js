import { apiFetch } from './client'

/**
 * 取得所有商品列表。
 *
 * 對應 API：GET /api/products
 *
 * @returns {Promise<Array>} 商品陣列，每筆包含 id、name、description、price、stock、imageUrl、createdAt
 */
export async function getProducts() {
  return apiFetch('/api/products')
}

/**
 * 取得單一商品詳細資料。
 *
 * 對應 API：GET /api/products/{id}
 *
 * @param {string} id - 商品 UUID
 * @returns {Promise<Object>} 商品物件，包含 id、name、description、price、stock、imageUrl、createdAt
 * @throws {Error} 商品不存在時 message 為伺服器回傳訊息（404）
 */
export async function getProduct(id) {
  return apiFetch(`/api/products/${id}`)
}
