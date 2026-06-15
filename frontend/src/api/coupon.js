import { apiFetch } from './client'

/**
 * 驗證優惠券代碼並試算折扣金額。
 * 不消耗使用次數，僅用於前端即時顯示折扣。
 * 折扣金額由伺服器計算後回傳，前端不得自行計算。
 *
 * 對應 API：POST /api/coupons/validate
 *
 * @param {string} code - 優惠券代碼（不分大小寫）
 * @param {number} orderAmount - 訂單原始金額（購物車合計）
 * @returns {Promise<Object>} 試算結果：
 *   - code {string} 優惠券代碼
 *   - name {string} 優惠券名稱
 *   - description {string} 說明
 *   - discountType {string} PERCENTAGE | FIXED
 *   - discountValue {number} 折扣值
 *   - discountAmount {number} 折扣金額（伺服器計算）
 *   - originalAmount {number} 原始訂單金額
 *   - finalAmount {number} 折扣後金額
 * @throws {Error} 優惠券不存在（404）或驗證失敗（400）
 */
export async function validateCoupon(code, orderAmount) {
  return apiFetch('/api/coupons/validate', {
    method: 'POST',
    body: JSON.stringify({ code, orderAmount }),
  })
}

/**
 * 查詢目前可用的優惠券清單（啟用中、在有效期限內、未超過次數上限）。
 * 供前端結帳頁顯示可選擇的優惠券。
 *
 * 對應 API：GET /api/coupons/available
 *
 * @returns {Promise<Array>} 可用優惠券清單，每筆含：
 *   - code {string}
 *   - name {string}
 *   - description {string}
 *   - discountType {string}
 *   - discountValue {number}
 *   - minOrderAmount {number}
 *   - endDate {string} ISO-8601 截止日期
 */
export async function getAvailableCoupons() {
  return apiFetch('/api/coupons/available')
}
