import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import './CheckoutSuccessPage.css'

/**
 * 結帳成功頁（T14）。
 *
 * 從 React Router 的 location.state 取得 orderSummary（訂單摘要），
 * 顯示訂單明細（品項列表、總金額）與結帳時間。
 *
 * 行為：
 * - 若直接訪問此頁（無 location.state），自動導向首頁（/）
 * - 「繼續購物」按鈕導向首頁（/）
 *
 * orderSummary 結構（來自 API §5）：
 * - cartId        {string}  購物車 UUID
 * - checkedOutAt  {string}  結帳時間（ISO-8601）
 * - recipient     {Object}  收件人資料（name、phone、email、address）
 * - items         {Array}   訂單明細（name、unitPrice、quantity、subtotal）
 * - total         {number}  訂單總金額
 *
 * @returns {JSX.Element}
 */
export default function CheckoutSuccessPage() {
  const location = useLocation()
  const navigate = useNavigate()

  /** 從 location.state 取得訂單摘要；直接訪問時為 null/undefined */
  const orderSummary = location.state?.orderSummary

  /**
   * 若無訂單資料（直接訪問或重新整理），立即導向首頁。
   */
  useEffect(() => {
    if (!orderSummary) {
      navigate('/', { replace: true })
    }
  }, [orderSummary, navigate])

  /* 無資料時不渲染任何內容（等待導向） */
  if (!orderSummary) return null

  /**
   * 格式化 ISO-8601 時間字串為本地化時間顯示。
   *
   * @param {string} isoString - ISO-8601 格式時間字串
   * @returns {string} 本地化時間字串
   */
  function formatDateTime(isoString) {
    return new Date(isoString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <main className="success-container">
      {/* ── 成功標題區 ── */}
      <div className="success-header">
        <div className="success-icon" aria-hidden="true">✓</div>
        <h1 className="success-title">Order Confirmed!</h1>
        <p className="success-subtitle">Thank you for your purchase.</p>
      </div>

      {/* ── 訂單摘要卡片 ── */}
      <section className="success-card" aria-label="Order Summary">
        <h2 className="success-card__heading">Order Summary</h2>

        {/* ── 結帳時間 ── */}
        <p className="success-card__meta">
          Ordered: {formatDateTime(orderSummary.checkedOutAt)}
        </p>

        {/* ── 收件人資訊 ── */}
        <div className="success-recipient">
          <h3 className="success-recipient__heading">Ship To</h3>
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

        {/* ── 品項列表 ── */}
        <table className="success-items" aria-label="Order items">
          <thead>
            <tr>
              <th className="success-items__col--name">Item</th>
              <th className="success-items__col--price">Price</th>
              <th className="success-items__col--qty">Qty</th>
              <th className="success-items__col--subtotal">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            {orderSummary.items.map((item, index) => (
              <tr key={item.productId ?? index}>
                <td>{item.name}</td>
                <td>NT${Number(item.unitPrice).toLocaleString()}</td>
                <td>{item.quantity}</td>
                <td>NT${Number(item.subtotal).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* ── 訂單總金額 ── */}
        <div className="success-total">
          <span className="success-total__label">Order Total</span>
          <span className="success-total__amount">
            NT${Number(orderSummary.total).toLocaleString()}
          </span>
        </div>
      </section>

      {/* ── 繼續購物按鈕 ── */}
      <button
        type="button"
        className="success-continue-btn"
        onClick={() => navigate('/')}
      >
        Continue Shopping
      </button>
    </main>
  )
}
