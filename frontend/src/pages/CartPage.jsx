import { useCallback, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import './CartPage.css'

/**
 * 單一購物車明細列元件。
 *
 * 負責呈現商品名稱、單價、數量增減按鈕、小計，
 * 並處理數量降為 0 時的淡出動畫後再呼叫 removeItem。
 *
 * @param {Object}   props
 * @param {Object}   props.item         - 購物車明細（itemId, name, unitPrice, quantity, subtotal）
 * @param {Function} props.onDecrease   - 點擊 − 時的 callback
 * @param {Function} props.onIncrease   - 點擊 + 時的 callback
 * @param {boolean}  props.isRemoving   - 是否正在淡出（控制 CSS 動畫）
 * @param {boolean}  props.loadingDec   - − 按鈕是否 loading
 * @param {boolean}  props.loadingInc   - + 按鈕是否 loading
 */
function CartItemRow({ item, onDecrease, onIncrease, isRemoving, loadingDec, loadingInc }) {
  const isLoading = loadingDec || loadingInc

  return (
    <li
      className={`cart-item-row${isRemoving ? ' cart-item-row--removing' : ''}`}
      aria-label={`${item.name}, $${item.unitPrice} each, qty ${item.quantity}, subtotal $${item.subtotal}`}
    >
      {/* 商品名稱 */}
      <span className="cart-item-name">{item.name}</span>

      {/* 單價 */}
      <span className="cart-item-unit-price">${item.unitPrice}</span>

      {/* 數量增減控制區 */}
      <div className="cart-item-qty">
        <button
          className="qty-btn"
          onClick={onDecrease}
          disabled={isLoading}
          aria-label={`Decrease ${item.name} quantity`}
        >
          −
        </button>
        <span className="qty-value" aria-live="polite">{item.quantity}</span>
        <button
          className="qty-btn"
          onClick={onIncrease}
          disabled={isLoading}
          aria-label={`Increase ${item.name} quantity`}
        >
          +
        </button>
      </div>

      {/* 小計（伺服器回傳值） */}
      <span className="cart-item-subtotal">${item.subtotal}</span>
    </li>
  )
}

/**
 * 購物車頁（CartPage）。
 *
 * 功能：
 * - 從 CartContext 取得 cart（含 items、total）
 * - 購物車為空時顯示提示與「繼續購物」連結
 * - 列出每個 CartItem 並提供數量增減按鈕
 *   - − 按鈕：quantity − 1，降至 0 時淡出後呼叫 removeItem(itemId)
 *   - + 按鈕：quantity + 1 → 呼叫 updateItem(itemId, quantity + 1)
 *   - 按鈕操作期間 loading 防連點
 * - 底部顯示伺服器回傳的 total（不自行加總）
 * - 「前往結帳」按鈕導向 /checkout，購物車空時 disabled
 * - 操作錯誤時以 alert 顯示 error.message
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

  /**
   * 設定特定明細的 loading 狀態。
   *
   * @param {string}      itemId    - 購物車明細 UUID
   * @param {'dec'|'inc'|null} dir  - 按鈕方向或 null（代表解除 loading）
   */
  const setLoading = useCallback((itemId, dir) => {
    setLoadingMap(prev => ({ ...prev, [itemId]: dir }))
  }, [])

  /**
   * 處理「− 減少數量」按鈕點擊。
   *
   * 若 quantity > 1：呼叫 updateItem(itemId, quantity - 1)
   * 若 quantity === 1：先觸發淡出動畫（300ms），再呼叫 removeItem(itemId)
   *
   * @param {Object} item - 購物車明細
   */
  const handleDecrease = useCallback(async (item) => {
    const { itemId, quantity } = item

    // 防止重複點擊
    if (loadingMap[itemId]) return
    setLoading(itemId, 'dec')

    try {
      if (quantity <= 1) {
        // 啟動淡出動畫後再移除
        setRemovingSet(prev => new Set([...prev, itemId]))
        // 等待 CSS transition 完成（300ms）
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
      // 若移除失敗，取消淡出狀態
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
   *
   * @param {Object} item - 購物車明細
   */
  const handleIncrease = useCallback(async (item) => {
    const { itemId, quantity } = item

    // 防止重複點擊
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

  /**
   * 前往結帳頁。
   */
  const handleCheckout = useCallback(() => {
    navigate('/checkout')
  }, [navigate])

  // cart 尚未載入時顯示 loading
  if (cart === null) {
    return (
      <div className="cart-page">
        <p className="cart-loading">Loading...</p>
      </div>
    )
  }

  const isEmpty = !cart.items || cart.items.length === 0

  return (
    <div className="cart-page">
      <h1 className="cart-title">Shopping Cart</h1>

      {isEmpty ? (
        /* ── 購物車為空 ── */
        <div className="cart-empty">
          <p className="cart-empty-msg">Your cart is empty</p>
          <Link to="/" className="cart-continue-link">Continue Shopping</Link>
        </div>
      ) : (
        /* ── 購物車明細列表 ── */
        <>
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

          {/* ── 分隔線 ── */}
          <hr className="cart-divider" />

          {/* ── 合計區塊（伺服器回傳值，不自行加總） ── */}
          <div className="cart-summary" aria-label={`Cart total: $${cart.total}`}>
            <span className="cart-summary-label">Order Total</span>
            <span className="cart-summary-total">${cart.total}</span>
          </div>
        </>
      )}

      {/* ── 前往結帳按鈕 ── */}
      <div className="cart-actions">
        <button
          className="cart-checkout-btn"
          onClick={handleCheckout}
          disabled={isEmpty}
          aria-disabled={isEmpty}
        >
          Checkout
        </button>
      </div>
    </div>
  )
}
