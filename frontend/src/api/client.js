/**
 * API 基底 URL，從環境變數讀取。
 * 開發時搭配 vite proxy，預設為空字串（請求直接打 /api/...，
 * 由 vite dev server proxy 轉發至 localhost:8083）。
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * 統一的 fetch 封裝函式。
 *
 * 功能：
 * - 自動補上 BASE_URL 前綴
 * - 所有請求帶 credentials: 'include'，確保 SESSION_ID cookie 隨請求傳送
 * - 預設 Content-Type: application/json
 * - 非 2xx 狀態碼時，從回應 body 取 message 欄位並 throw Error
 *
 * @param {string} path - API 路徑，例如 '/api/products'
 * @param {RequestInit} [options={}] - fetch 選項（method、body 等）
 * @returns {Promise<any>} 解析後的 JSON 回應；204 No Content 回傳 null
 */
export async function apiFetch(path, options = {}) {
  const url = `${BASE_URL}${path}`

  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers ?? {}),
  }

  const response = await fetch(url, {
    ...options,
    headers,
    credentials: 'include',
  })

  // 204 No Content：無回應 body，直接回傳 null
  if (response.status === 204) {
    return null
  }

  // 嘗試解析 JSON body（無論成功或失敗都需要）
  let data
  try {
    data = await response.json()
  } catch {
    // 若 body 無法解析為 JSON，建立空物件
    data = {}
  }

  // 非 2xx：取 message 欄位 throw Error
  if (!response.ok) {
    const message = data?.message ?? `HTTP Error ${response.status}`
    throw new Error(message)
  }

  return data
}
