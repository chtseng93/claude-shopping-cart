import { useState } from 'react'
import { validateCoupon, getAvailableCoupons } from '../api/coupon'
import './CouponInput.css'

/**
 * CouponInput — 優惠券輸入與選擇元件（結帳頁使用）。
 *
 * 功能：
 * 1. 手動輸入優惠券代碼並試算折扣
 * 2. 顯示可用優惠券清單（懶加載）供選擇
 * 3. 即時顯示折扣金額（由 API 回傳，非前端計算）
 * 4. 錯誤訊息顯示（無效、過期、不符條件等）
 *
 * 父元件需提供：
 * @param {number} orderAmount - 訂單原始金額（購物車合計，由父元件取得）
 * @param {Function} onCouponApplied - 優惠券套用成功時的回呼，傳入 { code, discountAmount, finalAmount }
 * @param {Function} onCouponCleared - 優惠券清除時的回呼
 */
export default function CouponInput({ orderAmount, onCouponApplied, onCouponCleared }) {
  /** 使用者手動輸入的優惠券代碼 */
  const [code, setCode] = useState('')

  /** 試算中狀態（防連點） */
  const [validating, setValidating] = useState(false)

  /** 試算結果（由 API 回傳，非前端計算） */
  const [result, setResult] = useState(null)

  /** API 回傳的錯誤訊息 */
  const [errorMsg, setErrorMsg] = useState(null)

  /** 可用優惠券清單（懶加載） */
  const [availableCoupons, setAvailableCoupons] = useState(null)

  /** 是否正在載入可用清單 */
  const [loadingList, setLoadingList] = useState(false)

  /** 是否展開可用清單面板 */
  const [showList, setShowList] = useState(false)

  /**
   * 載入可用優惠券清單（首次展開時才呼叫 API，後續使用快取）。
   */
  async function loadAvailableCoupons() {
    if (availableCoupons !== null) {
      // 已有快取，直接切換顯示/隱藏
      setShowList((prev) => !prev)
      return
    }
    setLoadingList(true)
    try {
      const list = await getAvailableCoupons()
      setAvailableCoupons(list)
      setShowList(true)
    } catch {
      setAvailableCoupons([])
    } finally {
      setLoadingList(false)
    }
  }

  /**
   * 點擊可用清單中的優惠券，自動填入代碼並觸發試算。
   *
   * @param {string} selectedCode - 選擇的優惠券代碼
   */
  async function handleSelectCoupon(selectedCode) {
    setCode(selectedCode)
    setShowList(false)
    await applyCoupon(selectedCode)
  }

  /**
   * 試算優惠券折扣。
   * 呼叫 POST /api/coupons/validate，取得伺服器計算的折扣金額。
   * 不消耗使用次數，僅用於即時顯示。
   *
   * @param {string} [inputCode] - 欲試算的代碼（預設使用 state.code）
   */
  async function applyCoupon(inputCode) {
    const targetCode = (inputCode ?? code).trim()
    if (!targetCode) {
      setErrorMsg('Please enter a coupon code')
      return
    }

    setValidating(true)
    setErrorMsg(null)
    setResult(null)

    try {
      // 折扣金額由伺服器計算，不在前端自行計算
      const data = await validateCoupon(targetCode, orderAmount)
      setResult(data)
      // 通知父元件已套用優惠券（提供折扣資訊）
      if (onCouponApplied) {
        onCouponApplied({
          code: data.code,
          discountAmount: data.discountAmount,
          finalAmount: data.finalAmount,
        })
      }
    } catch (err) {
      // 顯示伺服器回傳的錯誤訊息（驗證失敗原因）
      setErrorMsg(err.message || 'Coupon validation failed, please try again')
      setResult(null)
      if (onCouponCleared) onCouponCleared()
    } finally {
      setValidating(false)
    }
  }

  /**
   * 清除已套用的優惠券，回復無折扣狀態。
   */
  function clearCoupon() {
    setCode('')
    setResult(null)
    setErrorMsg(null)
    if (onCouponCleared) onCouponCleared()
  }

  /**
   * 按下 Enter 時觸發試算。
   *
   * @param {React.KeyboardEvent} e
   */
  function handleKeyDown(e) {
    if (e.key === 'Enter') {
      e.preventDefault()
      applyCoupon()
    }
  }

  return (
    <div className="coupon-input" aria-label="Coupon section">

      {/* ── 標題列 ── */}
      <div className="coupon-input__header">
        {/* 票券圖示（SVG） */}
        <svg className="coupon-input__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/>
          <path d="M13 5v14"/>
        </svg>
        <span className="coupon-input__title">Discount Code</span>
      </div>

      {/* ── 輸入區 ── */}
      {!result ? (
        <>
          <div className="coupon-input__row">
            <input
              type="text"
              className={`coupon-input__field${errorMsg ? ' coupon-input__field--error' : ''}`}
              value={code}
              onChange={(e) => {
                setCode(e.target.value.toUpperCase())
                setErrorMsg(null)
              }}
              onKeyDown={handleKeyDown}
              placeholder="Enter coupon code"
              maxLength={50}
              aria-label="Coupon code input"
              aria-describedby={errorMsg ? 'coupon-error' : undefined}
              disabled={validating}
            />
            <button
              type="button"
              className="coupon-input__apply-btn"
              onClick={() => applyCoupon()}
              disabled={validating || !code.trim()}
              aria-busy={validating}
            >
              {validating ? 'Verifying...' : 'Apply'}
            </button>
          </div>

          {/* 錯誤訊息 */}
          {errorMsg && (
            <p id="coupon-error" className="coupon-input__error" role="alert">
              {/* 警告圖示 */}
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              {errorMsg}
            </p>
          )}

          {/* 可用優惠券清單入口 */}
          <button
            type="button"
            className="coupon-input__list-toggle"
            onClick={loadAvailableCoupons}
            disabled={loadingList}
          >
            {loadingList ? 'Loading...' : showList ? '▲ Hide coupons' : '▼ View available coupons'}
          </button>

          {/* 可用優惠券清單面板 */}
          {showList && availableCoupons && (
            <ul className="coupon-input__list" role="list" aria-label="Available coupons">
              {availableCoupons.length === 0 ? (
                <li className="coupon-input__list-empty">No coupons available</li>
              ) : (
                availableCoupons.map((c) => (
                  <li key={c.code} className="coupon-input__list-item">
                    <button
                      type="button"
                      className="coupon-input__list-btn"
                      onClick={() => handleSelectCoupon(c.code)}
                      aria-label={`Apply coupon ${c.code}: ${c.name}`}
                    >
                      <span className="coupon-input__list-code">{c.code}</span>
                      <span className="coupon-input__list-name">{c.name}</span>
                      {c.description && (
                        <span className="coupon-input__list-desc">{c.description}</span>
                      )}
                      {c.minOrderAmount > 0 && (
                        <span className="coupon-input__list-min">
                          Min. order NT${Number(c.minOrderAmount).toLocaleString()}
                        </span>
                      )}
                    </button>
                  </li>
                ))
              )}
            </ul>
          )}
        </>
      ) : (
        /* ── 折扣試算結果顯示 ── */
        <div className="coupon-input__result" role="status" aria-live="polite">
          <div className="coupon-input__result-header">
            {/* 打勾圖示 */}
            <svg className="coupon-input__result-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
            <span className="coupon-input__result-name">{result.name}</span>
            <button
              type="button"
              className="coupon-input__clear-btn"
              onClick={clearCoupon}
              aria-label="Remove coupon"
            >
              ×
            </button>
          </div>
          <div className="coupon-input__result-rows">
            <div className="coupon-input__result-row">
              <span>Coupon Code</span>
              <span className="coupon-input__result-code">{result.code}</span>
            </div>
            <div className="coupon-input__result-row coupon-input__result-row--discount">
              <span>Discount</span>
              {/* 折扣金額由 API 回傳，非前端計算 */}
              <span>- NT${Number(result.discountAmount).toLocaleString()}</span>
            </div>
            <div className="coupon-input__result-row coupon-input__result-row--total">
              <span>Total After Discount</span>
              <span>NT${Number(result.finalAmount).toLocaleString()}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
