import { forwardRef, useImperativeHandle } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import useCartAnimation from '../hooks/useCartAnimation'
import Toast from './Toast'
import './NavBar.css'

/**
 * 共用導覽列元件（NavBar）。
 *
 * 版面說明：
 * - 左側：Logo 文字「🛒 Shopping Cart」，點擊導向首頁 `/`
 * - 右側：「購物車」文字連結（導向 `/cart`）+ 購物車圖示 + 數量徽章
 *
 * 徽章規則：
 * - 顯示 totalQuantity（購物車總件數），來自 CartContext
 * - 件數為 0 時以 display:none 隱藏徽章
 * - aria-label 隨件數動態更新，滿足無障礙需求
 *
 * 小互動（透過 ref 觸發）：
 * - 外部元件呼叫 navRef.current.triggerCartAnimation(message) 可同時觸發：
 *   1. 徽章 scale 彈跳動畫（300ms CSS keyframe）
 *   2. 購物車圖示晃動動畫（300ms CSS keyframe）
 *   3. 右下角 Toast 提示，2 秒後自動消失
 *
 * @example
 * // 在 App.jsx 或 ProductCard 中搭配加入購物車操作使用：
 * const navRef = useRef()
 * navRef.current?.triggerCartAnimation('已加入購物車')
 * <NavBar ref={navRef} />
 */
const NavBar = forwardRef(function NavBar(_props, ref) {
  /** 從 CartContext 取得購物車總件數 */
  const { totalQuantity } = useCart()

  /**
   * 動畫狀態管理 hook。
   * - badgeAnimating：是否正在執行徽章彈跳動畫
   * - triggerAnimation：觸發徽章彈跳（300ms）
   * - showToast / toastMessage：Toast 顯示狀態與訊息
   * - triggerToast：顯示 Toast 並 2 秒後自動隱藏
   */
  const {
    badgeAnimating,
    triggerAnimation,
    showToast,
    toastMessage,
    triggerToast,
  } = useCartAnimation()

  /**
   * 暴露給父元件（透過 ref）的公開 API。
   *
   * @method triggerCartAnimation
   * @param {string} [message='已加入購物車'] - Toast 要顯示的文字
   */
  useImperativeHandle(ref, () => ({
    /**
     * 同時觸發徽章彈跳動畫與 Toast 提示。
     * 供商品卡片「加入購物車」成功後呼叫。
     *
     * @param {string} [message='已加入購物車']
     */
    triggerCartAnimation(message = 'Added to cart') {
      triggerAnimation()
      triggerToast(message)
    },
  }))

  /** 是否顯示徽章（件數 > 0 才顯示） */
  const showBadge = totalQuantity > 0

  return (
    <>
      {/* ── 導覽列主體 ── */}
      <nav className="navbar" aria-label="Main navigation">
        <div className="navbar__inner">

        {/* ── 左側 Logo ── */}
        <Link to="/" className="navbar__logo">
          FurnitureCo.
        </Link>

        {/* ── 右側操作區 ── */}
        <div className="navbar__actions">

          {/* 購物車連結（含圖示、文字、徽章） */}
          <Link
            to="/cart"
            className="navbar__cart-link"
            aria-label={showBadge ? `Cart (${totalQuantity} items)` : 'Cart'}
          >
            {/* 購物車圖示：加入成功時觸發晃動動畫 */}
            <span
              className={`navbar__cart-icon${badgeAnimating ? ' navbar__cart-icon--shake' : ''}`}
              aria-hidden="true"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
                <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                <line x1="3" y1="6" x2="21" y2="6"/>
                <path d="M16 10a4 4 0 0 1-8 0"/>
              </svg>
            </span>

            {/* 連結文字 */}
            <span>Cart</span>

            {/* 數量徽章：件數為 0 時以 display:none 隱藏 */}
            <span
              className={`navbar__badge${badgeAnimating ? ' navbar__badge--bounce' : ''}`}
              aria-label={`Cart: ${totalQuantity} items`}
              style={{ display: showBadge ? undefined : 'none' }}
            >
              {totalQuantity}
            </span>
          </Link>

        </div>
        </div>
      </nav>

      {/* ── 右下角 Toast 浮動提示 ── */}
      <Toast message={toastMessage} show={showToast} />
    </>
  )
})

export default NavBar
