import { forwardRef, useImperativeHandle } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import useCartAnimation from '../hooks/useCartAnimation'
import Toast from './Toast'
import './NavBar.css'

/**
 * 共用導覽列元件（NavBar）— FurnitureCo. 重設計版。
 *
 * 版面說明：
 * - 左側：FurnitureCo. Logo（Playfair Display），點擊導向首頁 `/`
 * - 中間（桌面）：Shop / Collections / Sustainability / Journal 導覽連結
 * - 右側：search、favorite（Material Symbols）、shopping_bag（購物袋連結含徽章）
 *
 * 徽章規則：
 * - 顯示 totalQuantity（購物車總件數），來自 CartContext
 * - 件數為 0 時以 display:none 隱藏徽章
 *
 * 小互動（透過 ref 觸發）：
 * - triggerCartAnimation(message)：徽章彈跳 + 購物車圖示晃動 + Toast 提示
 *
 * @param {Object} _props
 * @param {React.Ref} ref
 */
const NavBar = forwardRef(function NavBar(_props, ref) {
  const { totalQuantity } = useCart()
  const location = useLocation()

  const {
    badgeAnimating,
    triggerAnimation,
    showToast,
    toastMessage,
    triggerToast,
  } = useCartAnimation()

  useImperativeHandle(ref, () => ({
    /**
     * 同時觸發徽章彈跳動畫與 Toast 提示。
     * @param {string} [message='Added to cart']
     */
    triggerCartAnimation(message = 'Added to cart') {
      triggerAnimation()
      triggerToast(message)
    },
  }))

  const showBadge = totalQuantity > 0

  return (
    <>
      <nav className="navbar" aria-label="Main navigation">
        <div className="navbar__inner">

          {/* ── 左側：Logo + 導覽連結 ── */}
          <div className="navbar__left">
          <Link to="/" className="navbar__logo">
            FurnitureCo.
          </Link>

          {/* 導覽連結（桌面）*/}
          <ul className="navbar__links" role="list">
            <li>
              <Link
                to="/"
                className={`navbar__link${location.pathname === '/' ? ' navbar__link--active' : ''}`}
              >
                Shop
              </Link>
            </li>
            <li><a href="#" className="navbar__link">Collections</a></li>
            <li><a href="#" className="navbar__link">Sustainability</a></li>
            <li><a href="#" className="navbar__link">Journal</a></li>
          </ul>
          </div>

          {/* ── 右側：圖示 + 購物車 ── */}
          <div className="navbar__actions">

            {/* 搜尋圖示 */}
            <button className="navbar__icon-btn" aria-label="Search">
              <span className="material-symbols-outlined">search</span>
            </button>

            {/* 收藏圖示 */}
            <button className="navbar__icon-btn" aria-label="Wishlist">
              <span className="material-symbols-outlined">favorite</span>
            </button>

            {/* 購物袋（購物車連結）+ 徽章 */}
            <Link
              to="/cart"
              className="navbar__cart-link"
              aria-label={showBadge ? `Cart (${totalQuantity} items)` : 'Cart'}
            >
              <span
                className={`navbar__cart-icon${badgeAnimating ? ' navbar__cart-icon--shake' : ''}`}
                aria-hidden="true"
              >
                <span className="material-symbols-outlined">shopping_bag</span>
              </span>

              {/* 數量徽章 */}
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
