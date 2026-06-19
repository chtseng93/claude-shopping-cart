import { useCallback, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import './CartPage.css'

/**
 * 購物車商品縮圖元件。
 * 有 imageUrl 時顯示圖片，否則顯示暖色佔位。
 *
 * @param {{ imageUrl: string|null|undefined, name: string }} props
 */
function CartItemThumb({ imageUrl, name }) {
  const [imgError, setImgError] = useState(false)

  if (!imageUrl || imgError) {
    return (
      <div className="cart-item-thumb-placeholder" aria-hidden="true">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" aria-hidden="true">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
    )
  }

  return (
    <img
      className="cart-item-thumb"
      src={imageUrl}
      alt={name}
      onError={() => setImgError(true)}
    />
  )
}

/**
 * 單一購物車商品列元件。
 *
 * 顯示縮圖、商品名稱、移除按鈕、單價、數量（pill 控制）、小計。
 *
 * @param {Object}   props
 * @param {Object}   props.item         - 購物車明細
 * @param {Function} props.onDecrease   - 點擊 − 時的 callback
 * @param {Function} props.onIncrease   - 點擊 + 時的 callback
 * @param {boolean}  props.isRemoving   - 是否正在淡出
 * @param {boolean}  props.loadingDec   - − 按鈕是否 loading
 * @param {boolean}  props.loadingInc   - + 按鈕是否 loading
 */
function CartItemRow({ item, onDecrease, onIncrease, isRemoving, loadingDec, loadingInc }) {
  const isLoading = loadingDec || loadingInc

  return (
    <li
      className={`cart-item-row${isRemoving ? ' cart-item-row--removing' : ''}`}
      aria-label={`${item.name}, NT$${item.unitPrice} each, qty ${item.quantity}`}
    >
      {/* 商品圖 + 名稱 + 移除按鈕 */}
      <div className="cart-item-info">
        <CartItemThumb imageUrl={item.imageUrl} name={item.name} />
        <div className="cart-item-meta">
          <span className="cart-item-name">{item.name}</span>
          {/* 行動裝置上顯示單價 */}
          <span className="cart-item-desc" style={{ display: 'none' }}>
            {/* 描述欄位暫無資料 */}
          </span>
          <button
            type="button"
            className="cart-item-remove"
            onClick={onDecrease}
            disabled={isLoading}
            aria-label={`Remove ${item.name} from cart`}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>close</span>
            Remove
          </button>
        </div>
      </div>

      {/* 單價 */}
      <span className="cart-item-unit-price" aria-label={`Unit price: NT$${item.unitPrice}`}>
        NT${Number(item.unitPrice).toLocaleString()}
      </span>

      {/* 數量增減 Pill */}
      <div className="cart-item-qty">
        <div className="qty-pill">
          <button
            type="button"
            className="qty-btn"
            onClick={onDecrease}
            disabled={isLoading}
            aria-label={`Decrease ${item.name} quantity`}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>remove</span>
          </button>
          <span className="qty-value" aria-live="polite">{item.quantity}</span>
          <button
            type="button"
            className="qty-btn"
            onClick={onIncrease}
            disabled={isLoading}
            aria-label={`Increase ${item.name} quantity`}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>add</span>
          </button>
        </div>
      </div>

      {/* 小計 */}
      <span className="cart-item-subtotal" aria-label={`Subtotal: NT$${item.subtotal}`}>
        NT${Number(item.subtotal).toLocaleString()}
      </span>
    </li>
  )
}

/**
 * 購物車頁（CartPage）— UI 重設計版。
 *
 * 功能與邏輯與原版相同，改為 12 欄雙欄佈局：
 * - 左 8 欄：商品列表（含縮圖）
 * - 右 4 欄：Order Summary sticky 側欄（Subtotal / Shipping / Tax / Total + 結帳按鈕）
 *
 * @component
 */
export default function CartPage() {
  const { cart, updateItem, removeItem } = useCart()
  const navigate = useNavigate()

  /**
   * 記錄各明細目前正在 loading 的按鈕方向。
   * key: itemId, value: 'dec' | 'inc' | null
   * @type {[Object.<string, string|null>, Function]}
   */
  const [loadingMap, setLoadingMap] = useState({})

  /**
   * 記錄正在淡出動畫中的明細 ID 集合。
   * @type {[Set<string>, Function]}
   */
  const [removingSet, setRemovingSet] = useState(new Set())

  /** 設定特定明細的 loading 狀態 */
  const setLoading = useCallback((itemId, dir) => {
    setLoadingMap(prev => ({ ...prev, [itemId]: dir }))
  }, [])

  /**
   * 處理「− 減少數量」按鈕點擊。
   * quantity > 1 → updateItem；quantity === 1 → 淡出後 removeItem。
   */
  const handleDecrease = useCallback(async (item) => {
    const { itemId, quantity } = item
    if (loadingMap[itemId]) return
    setLoading(itemId, 'dec')

    try {
      if (quantity <= 1) {
        setRemovingSet(prev => new Set([...prev, itemId]))
        await new Promise(resolve => setTimeout(resolve, 300))
        await removeItem(itemId)
        setRemovingSet(prev => {
          const next = new Set(prev)
          next.delete(itemId)
          return next
        })
      } else {
        await updateItem(itemId, quantity - 1)
      }
    } catch (error) {
      alert(error.message)
      setRemovingSet(prev => {
        const next = new Set(prev)
        next.delete(itemId)
        return next
      })
    } finally {
      setLoading(itemId, null)
    }
  }, [loadingMap, updateItem, removeItem, setLoading])

  /**
   * 處理「+ 增加數量」按鈕點擊。
   */
  const handleIncrease = useCallback(async (item) => {
    const { itemId, quantity } = item
    if (loadingMap[itemId]) return
    setLoading(itemId, 'inc')
    try {
      await updateItem(itemId, quantity + 1)
    } catch (error) {
      alert(error.message)
    } finally {
      setLoading(itemId, null)
    }
  }, [loadingMap, updateItem, setLoading])

  const [checkingOut, setCheckingOut] = useState(false)

  /** 短暫顯示按鈕變色後跳轉結帳頁 */
  const handleCheckout = useCallback(() => {
    setCheckingOut(true)
    setTimeout(() => navigate('/checkout'), 600)
  }, [navigate])

  /* ── 購物車尚未載入 ── */
  if (cart === null) {
    return (
      <div className="cart-page">
        <p className="cart-loading">Loading...</p>
      </div>
    )
  }

  const isEmpty = !cart.items || cart.items.length === 0

  /* ── 運費與稅金（UI 計算，不傳送至後端）── */
  const subtotal = cart.total ?? 0
  const shipping = subtotal > 0 ? 1500 : 0
  const tax = Math.round(subtotal * 0.05)
  const grandTotal = subtotal + shipping + tax

  return (
    <div className="cart-page">
      {/* 麵包屑 */}
      <nav className="cart-breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span>/</span>
        <span>Shopping Cart</span>
      </nav>

      <h1 className="cart-title">Shopping Cart</h1>

      {isEmpty ? (
        /* ── 購物車為空 ── */
        <div className="cart-empty">
          <span className="material-symbols-outlined cart-empty-icon" aria-hidden="true">shopping_cart_off</span>
          <p className="cart-empty-msg">Your cart is empty</p>
          <p className="cart-empty-desc">
            Looks like you haven't added anything yet. Discover our curated collections.
          </p>
          <Link to="/" className="cart-continue-link">Continue Shopping</Link>
        </div>
      ) : (
        <div className="cart-layout">
          {/* ════════════════════════════════════════
              左欄：商品列表
              ════════════════════════════════════════ */}
          <div className="cart-list-section">
            {/* 表頭（桌面）*/}
            <div className="cart-list-header" aria-hidden="true">
              <span>Product</span>
              <span className="cart-list-header--price">Price</span>
              <span className="cart-list-header--qty">Quantity</span>
              <span className="cart-list-header--subtotal">Subtotal</span>
            </div>

            <ul className="cart-list" role="list" aria-label="Cart items">
              {cart.items.map(item => (
                <CartItemRow
                  key={item.itemId}
                  item={item}
                  isRemoving={removingSet.has(item.itemId)}
                  loadingDec={loadingMap[item.itemId] === 'dec'}
                  loadingInc={loadingMap[item.itemId] === 'inc'}
                  onDecrease={() => handleDecrease(item)}
                  onIncrease={() => handleIncrease(item)}
                />
              ))}
            </ul>
          </div>

          {/* ════════════════════════════════════════
              右欄：Order Summary
              ════════════════════════════════════════ */}
          <div className="cart-summary" aria-label="Order Summary">
            <h2 className="cart-summary__title">Order Summary</h2>

            <div className="cart-summary__rows">
              <div className="cart-summary__row">
                <span>Subtotal</span>
                <span>NT${subtotal.toLocaleString()}</span>
              </div>
              <div className="cart-summary__row">
                <span>Shipping</span>
                <span>NT${shipping.toLocaleString()}</span>
              </div>
              <div className="cart-summary__row">
                <span>Estimated Tax</span>
                <span>NT${tax.toLocaleString()}</span>
              </div>
            </div>

            <hr className="cart-summary__divider" />

            <div className="cart-summary__total-row">
              <span className="cart-summary-label">Total</span>
              <span className="cart-summary-total">NT${grandTotal.toLocaleString()}</span>
            </div>

            <button
              type="button"
              className={`cart-checkout-btn${checkingOut ? ' cart-checkout-btn--checking' : ''}`}
              onClick={handleCheckout}
              disabled={isEmpty || checkingOut}
              aria-disabled={isEmpty}
            >
              Proceed to Checkout
            </button>

            <div className="cart-summary__perks">
              <div className="cart-summary__perk">
                <span className="material-symbols-outlined" style={{ fontSize: '20px', color: '#474741' }}>local_shipping</span>
                <span>Free delivery on orders over NT$100,000</span>
              </div>
              <div className="cart-summary__perk">
                <span className="material-symbols-outlined" style={{ fontSize: '20px', color: '#474741' }}>verified_user</span>
                <span>2-year manufacturer warranty</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
