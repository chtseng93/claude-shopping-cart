import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import CouponInput from '../components/CouponInput'
import './CheckoutPage.css'

/**
 * 結帳頁（T14）。
 *
 * 顯示收件人資料表單，欄位包含：姓名、電話、Email、地址。
 * 欄位採即時驗證（onBlur），格式不符顯示紅色錯誤訊息。
 * 送出鈕僅在表單完全有效時才 enabled。
 *
 * 流程：
 * 1. 若購物車為空，顯示提示並導回首頁（/）
 * 2. 使用者填寫表單並送出
 * 3. 呼叫 doCheckout(recipient)
 * 4. 成功 → 導向 /checkout/success，並透過 location.state 傳遞 orderSummary
 * 5. 失敗 → 在表單上方顯示紅色錯誤訊息區塊
 *
 * 狀態說明：
 * - fields      {Object}  各欄位的值
 * - touched     {Object}  各欄位是否已被觸碰（blur），用於控制是否顯示錯誤
 * - submitting  {boolean} 正在送出中（防連點）
 * - apiError    {string|null} API 回傳錯誤訊息
 */
export default function CheckoutPage() {
  /** 從 CartContext 取得購物車資料與結帳函式 */
  const { cart, doCheckout } = useCart()

  /** React Router 導向函式 */
  const navigate = useNavigate()

  /**
   * 各欄位的輸入值。
   * @type {[{name:string, phone:string, email:string, address:string}, Function]}
   */
  const [fields, setFields] = useState({
    name: '',
    phone: '',
    email: '',
    address: '',
  })

  /**
   * 各欄位是否已被觸碰（blur 過）。
   * 只有 touched 的欄位才顯示錯誤訊息，避免頁面一載入就全紅。
   * @type {[{name:boolean, phone:boolean, email:boolean, address:boolean}, Function]}
   */
  const [touched, setTouched] = useState({
    name: false,
    phone: false,
    email: false,
    address: false,
  })

  /** @type {[boolean, Function]} 是否正在送出（防止重複送出） */
  const [submitting, setSubmitting] = useState(false)

  /** @type {[string|null, Function]} API 回傳的錯誤訊息 */
  const [apiError, setApiError] = useState(null)

  /**
   * 已套用的優惠券代碼（null 表示未使用優惠券）。
   * 結帳時傳至後端，折扣金額由伺服器計算。
   * @type {[string|null, Function]}
   */
  const [appliedCouponCode, setAppliedCouponCode] = useState(null)

  /**
   * 優惠券試算結果（由 CouponInput 回呼傳入）。
   * 僅顯示用，實際折扣由伺服器在結帳時再次計算。
   * @type {[{discountAmount: number, finalAmount: number}|null, Function]}
   */
  const [couponResult, setCouponResult] = useState(null)

  /**
   * 購物車為空時顯示提示並在 1.5 秒後導回首頁。
   * cart 為 null 表示尚未載入，等待載入完成後再判斷。
   */
  useEffect(() => {
    if (cart !== null && cart.items.length === 0) {
      const timer = setTimeout(() => navigate('/'), 1500)
      return () => clearTimeout(timer)
    }
  }, [cart, navigate])

  /* ── 驗證規則 ── */

  /**
   * 姓名驗證：必填，非空。
   * @param {string} value
   * @returns {string} 錯誤訊息，無誤則回傳空字串
   */
  function validateName(value) {
    if (!value.trim()) return 'Name is required'
    return ''
  }

  /**
   * 電話驗證：必填，格式需符合台灣手機格式 09xxxxxxxx（10碼）。
   * @param {string} value
   * @returns {string} 錯誤訊息，無誤則回傳空字串
   */
  function validatePhone(value) {
    if (!value.trim()) return 'Phone is required'
    if (!/^09\d{8}$/.test(value.trim())) return 'Invalid format (09xxxxxxxx)'
    return ''
  }

  /**
   * Email 驗證：必填，需符合 Email 格式。
   * @param {string} value
   * @returns {string} 錯誤訊息，無誤則回傳空字串
   */
  function validateEmail(value) {
    if (!value.trim()) return 'Email is required'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())) return 'Invalid email format'
    return ''
  }

  /**
   * 地址驗證：必填，非空。
   * @param {string} value
   * @returns {string} 錯誤訊息，無誤則回傳空字串
   */
  function validateAddress(value) {
    if (!value.trim()) return 'Address is required'
    return ''
  }

  /**
   * 取得各欄位目前的錯誤訊息（不論是否已 touched）。
   * @returns {{name:string, phone:string, email:string, address:string}}
   */
  function getErrors() {
    return {
      name: validateName(fields.name),
      phone: validatePhone(fields.phone),
      email: validateEmail(fields.email),
      address: validateAddress(fields.address),
    }
  }

  /** 目前的所有欄位錯誤 */
  const errors = getErrors()

  /** 表單是否完全有效（所有欄位無錯誤訊息） */
  const isFormValid = Object.values(errors).every((e) => e === '')

  /**
   * 輸入值變更處理函式（通用）。
   * 清除 API 錯誤，以便使用者修正後重送。
   *
   * @param {React.ChangeEvent<HTMLInputElement|HTMLTextAreaElement>} e
   */
  function handleChange(e) {
    const { name, value } = e.target
    setFields((prev) => ({ ...prev, [name]: value }))
    setApiError(null)
  }

  /**
   * 欄位失焦（blur）處理函式（通用）。
   * 標記該欄位為已觸碰，觸發即時驗證顯示。
   *
   * @param {React.FocusEvent<HTMLInputElement|HTMLTextAreaElement>} e
   */
  function handleBlur(e) {
    const { name } = e.target
    setTouched((prev) => ({ ...prev, [name]: true }))
  }

  /**
   * 送出表單的處理函式。
   *
   * 1. 標記所有欄位為 touched（確保錯誤訊息顯示）
   * 2. 驗證失敗則中止
   * 3. 呼叫 doCheckout(recipient)
   * 4. 成功 → 導向 /checkout/success，傳遞 orderSummary
   * 5. 失敗 → 顯示 API 錯誤訊息
   *
   * @param {React.FormEvent<HTMLFormElement>} e
   */
  async function handleSubmit(e) {
    e.preventDefault()

    /* 標記所有欄位為已觸碰，確保顯示所有錯誤 */
    setTouched({ name: true, phone: true, email: true, address: true })

    /* 驗證失敗則不送出 */
    if (!isFormValid) return

    setSubmitting(true)
    setApiError(null)

    try {
      const orderSummary = await doCheckout(
        {
          name: fields.name.trim(),
          phone: fields.phone.trim(),
          email: fields.email.trim(),
          address: fields.address.trim(),
        },
        // 傳入已套用的優惠券代碼（null 表示不使用）
        // 折扣金額由伺服器計算，不傳入折扣金額
        appliedCouponCode
      )
      /* 成功：導向結帳成功頁，傳遞訂單摘要 */
      navigate('/checkout/success', { state: { orderSummary } })
    } catch (err) {
      setApiError(err.message || 'Checkout failed. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  /* ── 購物車尚未載入 ── */
  if (cart === null) {
    return (
      <main className="checkout-container">
        <p className="checkout-status">Loading...</p>
      </main>
    )
  }

  /* ── 購物車為空 ── */
  if (cart.items.length === 0) {
    return (
      <main className="checkout-container">
        <p className="checkout-status">Your cart is empty. Redirecting...</p>
      </main>
    )
  }

  /* ── 正常渲染：結帳表單 ── */
  return (
    <main className="checkout-container">
      <h1 className="checkout-title">Shipping Details</h1>

      {/* API 錯誤訊息區塊 */}
      {apiError && (
        <div className="checkout-api-error" role="alert">
          {apiError}
        </div>
      )}

      <form className="checkout-form" onSubmit={handleSubmit} noValidate>
        {/* ── 姓名 ── */}
        <div className="checkout-field">
          <label className="checkout-label" htmlFor="name">
            Name <span className="checkout-required" aria-hidden="true">*</span>
          </label>
          <input
            id="name"
            type="text"
            name="name"
            className={`checkout-input${touched.name && errors.name ? ' checkout-input--error' : ''}`}
            value={fields.name}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="John Doe"
            autoComplete="name"
            aria-required="true"
            aria-describedby={touched.name && errors.name ? 'name-error' : undefined}
          />
          {touched.name && errors.name && (
            <p id="name-error" className="checkout-error-msg" role="alert">
              {errors.name}
            </p>
          )}
        </div>

        {/* ── 電話 ── */}
        <div className="checkout-field">
          <label className="checkout-label" htmlFor="phone">
            Phone <span className="checkout-required" aria-hidden="true">*</span>
          </label>
          <input
            id="phone"
            type="tel"
            name="phone"
            className={`checkout-input${touched.phone && errors.phone ? ' checkout-input--error' : ''}`}
            value={fields.phone}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="0912345678"
            autoComplete="tel"
            aria-required="true"
            aria-describedby={touched.phone && errors.phone ? 'phone-error' : undefined}
          />
          {touched.phone && errors.phone && (
            <p id="phone-error" className="checkout-error-msg" role="alert">
              {errors.phone}
            </p>
          )}
        </div>

        {/* ── Email ── */}
        <div className="checkout-field">
          <label className="checkout-label" htmlFor="email">
            Email <span className="checkout-required" aria-hidden="true">*</span>
          </label>
          <input
            id="email"
            type="email"
            name="email"
            className={`checkout-input${touched.email && errors.email ? ' checkout-input--error' : ''}`}
            value={fields.email}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="user@example.com"
            autoComplete="email"
            aria-required="true"
            aria-describedby={touched.email && errors.email ? 'email-error' : undefined}
          />
          {touched.email && errors.email && (
            <p id="email-error" className="checkout-error-msg" role="alert">
              {errors.email}
            </p>
          )}
        </div>

        {/* ── 地址 ── */}
        <div className="checkout-field">
          <label className="checkout-label" htmlFor="address">
            Address <span className="checkout-required" aria-hidden="true">*</span>
          </label>
          <input
            id="address"
            type="text"
            name="address"
            className={`checkout-input${touched.address && errors.address ? ' checkout-input--error' : ''}`}
            value={fields.address}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="123 Main Street, City"
            autoComplete="street-address"
            aria-required="true"
            aria-describedby={touched.address && errors.address ? 'address-error' : undefined}
          />
          {touched.address && errors.address && (
            <p id="address-error" className="checkout-error-msg" role="alert">
              {errors.address}
            </p>
          )}
        </div>

        {/* ── 優惠券輸入區塊 ── */}
        {cart && cart.total > 0 && (
          <CouponInput
            orderAmount={cart.total}
            onCouponApplied={(info) => {
              // 記錄已套用的代碼（伺服器結帳時再次驗證計算）
              setAppliedCouponCode(info.code)
              setCouponResult(info)
              setApiError(null)
            }}
            onCouponCleared={() => {
              setAppliedCouponCode(null)
              setCouponResult(null)
            }}
          />
        )}

        {/* ── 訂單金額摘要（有優惠券時顯示折扣前/後） ── */}
        <div className="checkout-order-summary">
          <div className="checkout-order-summary__row">
            <span>Subtotal</span>
            <span>NT${cart ? Number(cart.total).toLocaleString() : '—'}</span>
          </div>
          {couponResult && (
            <div className="checkout-order-summary__row checkout-order-summary__row--discount">
              <span>Discount ({appliedCouponCode})</span>
              {/* 折扣金額來自試算 API 回傳，非前端計算 */}
              <span>- NT${Number(couponResult.discountAmount).toLocaleString()}</span>
            </div>
          )}
          <div className="checkout-order-summary__row checkout-order-summary__row--total">
            <span>Total</span>
            <span>
              NT${couponResult
                ? Number(couponResult.finalAmount).toLocaleString()
                : (cart ? Number(cart.total).toLocaleString() : '—')
              }
            </span>
          </div>
        </div>

        {/* ── 送出按鈕 ── */}
        <button
          type="submit"
          className="checkout-submit-btn"
          disabled={!isFormValid || submitting}
          aria-busy={submitting}
        >
          {submitting ? 'Processing...' : 'Place Order'}
        </button>
      </form>
    </main>
  )
}
