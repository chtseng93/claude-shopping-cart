import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import CouponInput from '../components/CouponInput'
import './CheckoutPage.css'

/**
 * 結帳頁（UI 重設計版）。
 *
 * 佈局：2 欄（7 + 5）
 * - 左欄：收件人表單（底邊框輸入框）、優惠券區塊（glassmorphism）
 * - 右欄：Sticky Order Summary（商品縮圖、金額明細、Place Order 按鈕 + SSL 標語）
 *
 * 邏輯保留原版：
 * - 即時欄位驗證（onBlur）
 * - doCheckout() → 導向 /checkout/success
 * - 優惠券試算透過 CouponInput 元件
 *
 * @component
 */
export default function CheckoutPage() {
  const { cart, doCheckout } = useCart()
  const navigate = useNavigate()

  /** 各欄位的輸入值 */
  const [fields, setFields] = useState({ name: '', phone: '', email: '', address: '' })

  /** 各欄位是否已被觸碰（blur），控制錯誤訊息顯示 */
  const [touched, setTouched] = useState({ name: false, phone: false, email: false, address: false })

  /** 是否正在送出（防止重複送出） */
  const [submitting, setSubmitting] = useState(false)

  /** API 回傳的錯誤訊息 */
  const [apiError, setApiError] = useState(null)

  /** 已套用的優惠券代碼 */
  const [appliedCouponCode, setAppliedCouponCode] = useState(null)

  /** 優惠券試算結果（僅顯示用） */
  const [couponResult, setCouponResult] = useState(null)

  /** 進入頁面時捲回頂部 */
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [])

  /** 購物車為空時，1.5 秒後導回首頁 */
  useEffect(() => {
    if (cart !== null && cart.items.length === 0) {
      const timer = setTimeout(() => navigate('/'), 1500)
      return () => clearTimeout(timer)
    }
  }, [cart, navigate])

  /* ── 驗證規則 ── */
  function validateName(v) { return v.trim() ? '' : 'Name is required' }
  function validatePhone(v) {
    if (!v.trim()) return 'Phone is required'
    if (!/^09\d{8}$/.test(v.trim())) return 'Invalid format (09xxxxxxxx)'
    return ''
  }
  function validateEmail(v) {
    if (!v.trim()) return 'Email is required'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim())) return 'Invalid email format'
    return ''
  }
  function validateAddress(v) { return v.trim() ? '' : 'Address is required' }

  function getErrors() {
    return {
      name: validateName(fields.name),
      phone: validatePhone(fields.phone),
      email: validateEmail(fields.email),
      address: validateAddress(fields.address),
    }
  }

  const errors = getErrors()
  const isFormValid = Object.values(errors).every((e) => e === '')

  function handleChange(e) {
    const { name, value } = e.target
    setFields((prev) => ({ ...prev, [name]: value }))
    setApiError(null)
  }
  function handleBlur(e) {
    const { name } = e.target
    setTouched((prev) => ({ ...prev, [name]: true }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setTouched({ name: true, phone: true, email: true, address: true })
    if (!isFormValid) return

    setSubmitting(true)
    setApiError(null)
    try {
      const orderSummary = await doCheckout(
        { name: fields.name.trim(), phone: fields.phone.trim(), email: fields.email.trim(), address: fields.address.trim() },
        appliedCouponCode
      )
      navigate('/checkout/success', { state: { orderSummary } })
    } catch (err) {
      setApiError(err.message || 'Checkout failed. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  /* ── 購物車尚未載入 ── */
  if (cart === null) {
    return <main className="checkout-page"><p className="checkout-status">Loading...</p></main>
  }

  /* ── 購物車為空 ── */
  if (cart.items.length === 0) {
    return <main className="checkout-page"><p className="checkout-status">Your cart is empty. Redirecting...</p></main>
  }

  /* 金額計算（顯示用）*/
  const displayTotal = couponResult ? couponResult.finalAmount : cart.total

  return (
    <main className="checkout-page">
      <div className="checkout-layout">

        {/* ════════════════════════════════════════
            左欄：收件人表單 + 優惠券
            ════════════════════════════════════════ */}
        <div className="checkout-left co-fade-up co-s1">
          <h1 className="checkout-title">Shipping Details</h1>

          {/* API 錯誤 */}
          {apiError && (
            <div className="checkout-api-error" role="alert">{apiError}</div>
          )}

          <form className="checkout-form" onSubmit={handleSubmit} noValidate>
            {/* 姓名 + 電話（兩欄）*/}
            <div className="checkout-form-row">
              <div className="checkout-field">
                <label className="checkout-label" htmlFor="name">
                  Full Name <span className="checkout-required" aria-hidden="true">*</span>
                </label>
                <input
                  id="name"
                  type="text"
                  name="name"
                  className={`checkout-input${touched.name && errors.name ? ' checkout-input--error' : ''}`}
                  value={fields.name}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  placeholder="E.g. John Doe"
                  autoComplete="name"
                  aria-required="true"
                  aria-describedby={touched.name && errors.name ? 'name-error' : undefined}
                />
                {touched.name && errors.name && (
                  <p id="name-error" className="checkout-error-msg" role="alert">{errors.name}</p>
                )}
              </div>

              <div className="checkout-field">
                <label className="checkout-label" htmlFor="phone">
                  Phone Number <span className="checkout-required" aria-hidden="true">*</span>
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
                  <p id="phone-error" className="checkout-error-msg" role="alert">{errors.phone}</p>
                )}
              </div>
            </div>

            {/* Email */}
            <div className="checkout-field">
              <label className="checkout-label" htmlFor="email">
                Email Address <span className="checkout-required" aria-hidden="true">*</span>
              </label>
              <input
                id="email"
                type="email"
                name="email"
                className={`checkout-input${touched.email && errors.email ? ' checkout-input--error' : ''}`}
                value={fields.email}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="you@example.com"
                autoComplete="email"
                aria-required="true"
                aria-describedby={touched.email && errors.email ? 'email-error' : undefined}
              />
              {touched.email && errors.email && (
                <p id="email-error" className="checkout-error-msg" role="alert">{errors.email}</p>
              )}
            </div>

            {/* 地址 */}
            <div className="checkout-field">
              <label className="checkout-label" htmlFor="address">
                Shipping Address <span className="checkout-required" aria-hidden="true">*</span>
              </label>
              <input
                id="address"
                type="text"
                name="address"
                className={`checkout-input${touched.address && errors.address ? ' checkout-input--error' : ''}`}
                value={fields.address}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="Street Address, District, City"
                autoComplete="street-address"
                aria-required="true"
                aria-describedby={touched.address && errors.address ? 'address-error' : undefined}
              />
              {touched.address && errors.address && (
                <p id="address-error" className="checkout-error-msg" role="alert">{errors.address}</p>
              )}
            </div>

            {/* ── 優惠券 ── */}
            {cart.total > 0 && (
              <div className="checkout-coupon-section">
                <h3>Offer Codes</h3>
                <div className="checkout-coupon-glass">
                  <CouponInput
                    orderAmount={cart.total}
                    onCouponApplied={(info) => {
                      setAppliedCouponCode(info.code)
                      setCouponResult(info)
                      setApiError(null)
                    }}
                    onCouponCleared={() => {
                      setAppliedCouponCode(null)
                      setCouponResult(null)
                    }}
                  />
                </div>
              </div>
            )}
          </form>
        </div>

        {/* ════════════════════════════════════════
            右欄：Order Summary（sticky）
            ════════════════════════════════════════ */}
        <div className="checkout-right co-fade-up co-s2">
          <div className="checkout-summary">
            <h2 className="checkout-summary__title">Order Summary</h2>

            {/* 商品縮圖列表 */}
            <div className="checkout-summary__items">
              {cart.items.map((item) => (
                <div key={item.itemId} className="checkout-summary__item">
                  <CartSummaryThumb imageUrl={item.imageUrl} name={item.name} />
                  <div className="checkout-summary__item-info">
                    <p className="checkout-summary__item-name">{item.name}</p>
                    <p className="checkout-summary__item-desc">Qty: {item.quantity}</p>
                    <p className="checkout-summary__item-price">
                      NT${Number(item.subtotal).toLocaleString()}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {/* 金額明細 */}
            <div className="checkout-order-summary">
              <div className="checkout-order-summary__row">
                <span>Subtotal</span>
                <span>NT${Number(cart.total).toLocaleString()}</span>
              </div>
              {couponResult && (
                <div className="checkout-order-summary__row checkout-order-summary__row--discount">
                  <span>Discount ({appliedCouponCode})</span>
                  <span>- NT${Number(couponResult.discountAmount).toLocaleString()}</span>
                </div>
              )}
              <div className="checkout-order-summary__row">
                <span>Shipping</span>
                <span>Free</span>
              </div>
              <div className="checkout-order-summary__row checkout-order-summary__row--total">
                <span>Total</span>
                <span>NT${Number(displayTotal).toLocaleString()}</span>
              </div>
            </div>

            {/* Place Order 按鈕（onClick 直接觸發 handleSubmit）*/}
            <button
              type="button"
              className="checkout-submit-btn"
              disabled={!isFormValid || submitting}
              aria-busy={submitting}
              onClick={handleSubmit}
            >
              {submitting ? 'Processing...' : 'Place Order'}
            </button>

            {/* SSL 標語 */}
            <div className="checkout-ssl-badge">
              <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>lock</span>
              Secure 256-bit SSL encrypted checkout
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}

/**
 * Order Summary 側欄的商品縮圖元件。
 *
 * @param {{ imageUrl: string|null|undefined, name: string }} props
 */
function CartSummaryThumb({ imageUrl, name }) {
  const [imgError, setImgError] = useState(false)

  if (!imageUrl || imgError) {
    return (
      <div className="checkout-summary__item-thumb-placeholder" aria-hidden="true">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" aria-hidden="true">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
    )
  }

  return (
    <img
      className="checkout-summary__item-thumb"
      src={imageUrl}
      alt={name}
      onError={() => setImgError(true)}
    />
  )
}
