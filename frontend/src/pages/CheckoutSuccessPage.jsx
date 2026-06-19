import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import './CheckoutSuccessPage.css'

/**
 * 結帳成功頁（UI 重設計版）。
 *
 * 設計稿：docs/confirmation.html
 *
 * 結構：
 * 1. Hero 成功區塊（SVG 打勾動畫 + 標題 + 副標題）
 * 2. 訂單摘要卡片（日期、收件人、品項表格、金額）
 * 3. Continue Shopping 圓角膠囊按鈕
 *
 * orderSummary（來自 location.state，由 API 回傳）：
 * - cartId、checkedOutAt、recipient、items、total
 *
 * @returns {JSX.Element}
 */
export default function CheckoutSuccessPage() {
  const location = useLocation()
  const navigate = useNavigate()

  /** 從 location.state 取得訂單摘要；直接訪問時為 null */
  const orderSummary = location.state?.orderSummary

  /** 若無訂單資料（直接訪問或重新整理），立即導向首頁 */
  useEffect(() => {
    if (!orderSummary) {
      navigate('/', { replace: true })
    }
  }, [orderSummary, navigate])

  /** 進入頁面時捲回頂部，確保 icon + 標題從頭顯示 */
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [])

  /* 無資料時不渲染任何內容（等待導向） */
  if (!orderSummary) return null

  /**
   * 格式化 ISO-8601 時間字串為本地化時間。
   * @param {string} isoString
   * @returns {string}
   */
  function formatDateTime(isoString) {
    return new Date(isoString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'long',
      day: '2-digit',
    })
  }

  return (
    <main className="success-container">
      <div className="success-inner">

        {/* ════════════════════════════════════════
            Hero 成功區塊
            ════════════════════════════════════════ */}
        <div className="success-header">
          {/* icon：stagger-1（delay 100ms）*/}
          <div className="success-icon-wrap success-fade-up success-stagger-1">
            <div className="success-icon-circle">
              <svg
                width="64"
                height="64"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#16A34A"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path className="success-checkmark-path" d="M20 6L9 17l-5-5" />
              </svg>
            </div>
          </div>

          {/* 標題：stagger-2（delay 300ms）*/}
          <h1 className="success-title success-fade-up success-stagger-2">Order Confirmed!</h1>
          {/* 副標題：stagger-3（delay 500ms）*/}
          <p className="success-subtitle success-fade-up success-stagger-3">
            Your Scandinavian-inspired sanctuary is just a shipment away.
            We've sent a receipt to your email.
          </p>
        </div>

        {/* ════════════════════════════════════════
            訂單摘要卡片（stagger-4，delay 700ms）
            ════════════════════════════════════════ */}
        <section className="success-card success-fade-up success-stagger-4" aria-label="Order Summary">

          {/* 日期 + 收件人 */}
          <div className="success-card__meta-grid">
            <div className="success-card__meta-item">
              <span className="success-card__meta-label">Ordered Date</span>
              <p className="success-card__meta-value">
                {formatDateTime(orderSummary.checkedOutAt)}
              </p>
            </div>

            <div className="success-card__meta-item">
              <span className="success-card__meta-label">Order ID</span>
              <p className="success-card__meta-value" style={{ fontFamily: 'monospace', fontSize: '13px' }}>
                #{orderSummary.cartId?.slice(0, 8).toUpperCase()}
              </p>
            </div>

            {/* 收件人（跨欄）*/}
            <div className="success-card__meta-item success-recipient" style={{ gridColumn: '1 / -1' }}>
              <span className="success-card__meta-label">Ship To Details</span>
              <dl className="success-recipient__list">
                <div className="success-recipient__row">
                  <dt>Name</dt>
                  <dd>{orderSummary.recipient.name}</dd>
                </div>
                <div className="success-recipient__row">
                  <dt>Phone</dt>
                  <dd>{orderSummary.recipient.phone}</dd>
                </div>
                <div className="success-recipient__row">
                  <dt>Email</dt>
                  <dd>{orderSummary.recipient.email}</dd>
                </div>
                <div className="success-recipient__row">
                  <dt>Address</dt>
                  <dd>{orderSummary.recipient.address}</dd>
                </div>
              </dl>
            </div>
          </div>

          {/* 品項表格 */}
          <div style={{ overflowX: 'auto' }}>
            <table className="success-items" aria-label="Order items">
              <thead>
                <tr>
                  <th className="success-items__col--name">Item</th>
                  <th className="success-items__col--qty">Qty</th>
                  <th className="success-items__col--price">Unit Price</th>
                  <th className="success-items__col--subtotal">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {orderSummary.items.map((item, index) => (
                  <tr key={item.productId ?? index}>
                    <td>
                      <div className="success-item-cell">
                        <div>
                          <p className="success-item-name">{item.name}</p>
                        </div>
                      </div>
                    </td>
                    <td style={{ textAlign: 'center' }}>{item.quantity}</td>
                    <td style={{ textAlign: 'center' }}>NT${Number(item.unitPrice).toLocaleString()}</td>
                    <td style={{ textAlign: 'right' }}>NT${Number(item.subtotal).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 金額摘要 */}
          <div className="success-total">
            <hr className="success-total__divider" />
            <div className="success-total__row">
              <span>Subtotal</span>
              <span>NT${Number(orderSummary.total).toLocaleString()}</span>
            </div>
            <div className="success-total__row">
              <span>Shipping</span>
              <span>Free</span>
            </div>
            <div className="success-total__row" style={{ fontFamily: '"Playfair Display", serif', fontSize: '24px', fontWeight: 500, color: '#1e1b18' }}>
              <span className="success-total__label">Total</span>
              <span className="success-total__amount">NT${Number(orderSummary.total).toLocaleString()}</span>
            </div>
          </div>
        </section>

        {/* ════════════════════════════════════════
            Continue Shopping CTA（stagger-5，delay 900ms）
            ════════════════════════════════════════ */}
        <div className="success-cta success-fade-up success-stagger-5">
          <button
            type="button"
            className="success-continue-btn"
            onClick={() => navigate('/')}
          >
            Continue Shopping
          </button>
          <p className="success-cta__help">
            Need help? <a href="#">Contact our Concierge</a>
          </p>
        </div>

      </div>
    </main>
  )
}
