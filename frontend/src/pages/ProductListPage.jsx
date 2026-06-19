import { useEffect, useMemo, useRef, useState } from 'react'
import { getProducts } from '../api/products'
import { useCart } from '../context/CartContext'
import './ProductListPage.css'

/**
 * 商品列表頁（UI 重設計版）。
 *
 * 頁面結構：
 * 1. Hero Banner — 70vh 影片背景，左側文案 + Glassmorphism 會員優惠卡
 * 2. Filter Bar — 分類 Pills（sticky），顯示商品數量
 * 3. 商品 Grid — 3 欄（桌面），每張卡 4/5 比例圖片、愛心按鈕、加入購物車
 *
 * 狀態說明：
 * - products      {Array}   商品列表（由 API 取得）
 * - loading       {boolean} 頁面初始載入中
 * - error         {string|null} 頁面初始載入失敗訊息
 * - activeFilter  {string}  目前選中的分類篩選（'All' 或分類名稱）
 * - addingMap     {Object}  key 為 productId，value 為 'loading' | 'done' | undefined
 */
export default function ProductListPage() {
  /** @type {[Array, Function]} 商品列表 */
  const [products, setProducts] = useState([])

  /** @type {[boolean, Function]} 頁面初始載入狀態 */
  const [loading, setLoading] = useState(true)

  /** @type {[string|null, Function]} 初始載入錯誤訊息 */
  const [error, setError] = useState(null)

  /** @type {[string, Function]} 目前選中的分類篩選 */
  const [activeFilter, setActiveFilter] = useState('All pieces')

  /** @type {[boolean, Function]} 是否展開全部商品（預設只顯示前 6 件）*/
  const [showAll, setShowAll] = useState(false)

  /** @type {[boolean, Function]} 優惠券按鈕是否已複製（短暫變色用）*/
  const [couponCopied, setCouponCopied] = useState(false)

  /**
   * 各商品加入購物車的按鈕狀態對照表。
   * key 為 productId，value 為 'loading'（請求中）| 'done'（成功 1.5s）| undefined（正常）
   * @type {[Object, Function]}
   */
  const [addingMap, setAddingMap] = useState({})

  /** 從 CartContext 取得 addItem 方法 */
  const { addItem } = useCart()

  /** 商品 Grid 的 ref，用於 IntersectionObserver 偵測卡片進入視窗 */
  const gridRef = useRef(null)

  /**
   * 頁面掛載時載入商品列表。
   * 載入失敗時設定 error 訊息。
   */
  useEffect(() => {
    async function fetchProducts() {
      try {
        const data = await getProducts()
        setProducts(data)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    fetchProducts()
  }, [])

  /** 從商品資料中萃取所有分類（去重），加上 'All' */
  const categories = useMemo(() => {
    const cats = [...new Set(products.map((p) => p.category).filter(Boolean))]
    return ['All pieces', ...cats]
  }, [products])

  /** 依目前 filter 過濾商品 */
  const filteredProducts = useMemo(() => {
    if (activeFilter === 'All pieces') return products
    return products.filter((p) => p.category === activeFilter)
  }, [products, activeFilter])

  /** 切換分類時重設展開狀態 */
  useEffect(() => {
    setShowAll(false)
  }, [activeFilter])

  /** 目前實際顯示的商品（未展開時最多 6 件）*/
  const displayedProducts = showAll ? filteredProducts : filteredProducts.slice(0, 6)

  /**
   * 商品列表渲染後，設定 IntersectionObserver 監聽每張卡片。
   * 卡片進入視窗時加 plp-card--visible 觸發入場動畫；
   * 動畫結束後移除 reveal class，讓 hover transform 正常作用。
   */
  useEffect(() => {
    if (!gridRef.current) return

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const el = entry.target
            el.classList.add('plp-card--visible')
            observer.unobserve(el)
            setTimeout(() => {
              el.classList.remove('plp-card--reveal', 'plp-card--visible')
            }, 800)
          }
        })
      },
      { threshold: 0.05, rootMargin: '0px' }
    )

    gridRef.current.querySelectorAll('.plp-card--reveal').forEach((el) => observer.observe(el))

    return () => observer.disconnect()
  }, [displayedProducts])

  /**
   * 點擊「加入購物車」按鈕的處理函式。
   *
   * @param {string} productId - 商品 UUID
   */
  async function handleAddToCart(productId) {
    if (addingMap[productId]) return

    setAddingMap((prev) => ({ ...prev, [productId]: 'loading' }))

    try {
      await addItem(productId, 1)
      setAddingMap((prev) => ({ ...prev, [productId]: 'done' }))

      setTimeout(() => {
        setAddingMap((prev) => {
          const next = { ...prev }
          delete next[productId]
          return next
        })
      }, 1500)
    } catch (err) {
      alert(err.message)
      setAddingMap((prev) => {
        const next = { ...prev }
        delete next[productId]
        return next
      })
    }
  }

  /* ── 頁面初始載入中 ── */
  if (loading) {
    return (
      <main className="plp-page">
        <p className="plp-status">Loading products...</p>
      </main>
    )
  }

  /* ── 初始載入失敗 ── */
  if (error) {
    return (
      <main className="plp-page">
        <p className="plp-status plp-status--error">Failed to load: {error}</p>
      </main>
    )
  }

  /* ── 正常渲染 ── */
  return (
    <main className="plp-page">

      {/* ════════════════════════════════════════
          Hero Section — 影片 Banner
          ════════════════════════════════════════ */}
      <section className="plp-hero-section" aria-label="Hero banner">
        {/* 全幅背景影片（覆蓋整個 hero 區域）*/}
        <div className="plp-hero-img-wrap" aria-hidden="true">
          <video
            className="plp-hero-img"
            src="/banner_video.mp4"
            autoPlay
            muted
            loop
            playsInline
          />
        </div>

        {/* 漸層 overlay */}
        <div className="plp-hero-overlay" aria-hidden="true" />

        {/* 文字 + 會員卡 */}
        <div className="plp-hero-content">
          <span className="plp-hero-eyebrow">New Season Collection</span>
          <h1 className="plp-hero-title">
            Thoughtfully Designed<br />for Modern Living
          </h1>
          <p className="plp-hero-subtitle">
            Warm textures, natural materials,<br />
            crafted for the spaces you love.
          </p>

          {/* Glassmorphism 會員優惠卡 */}
          <div className="plp-hero-card">
            <div className="plp-hero-card__row">
              <div className="plp-hero-card__icon" aria-hidden="true">
                <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>star</span>
              </div>
              <div>
                <span className="plp-hero-card__label">Member Exclusive</span>
                <span className="plp-hero-card__text">10% off your first order</span>
              </div>
            </div>
            <button
              type="button"
              className={`plp-hero-card__btn${couponCopied ? ' plp-hero-card__btn--copied' : ''}`}
              onClick={() => {
                navigator.clipboard?.writeText('WELCOME10').catch(() => {})
                setCouponCopied(true)
                setTimeout(() => setCouponCopied(false), 1500)
              }}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>
                {couponCopied ? 'check' : 'content_copy'}
              </span>
              <span className="plp-hero-card__btn-label">
                <span className={couponCopied ? 'plp-hero-card__btn-label--hidden' : ''}>Copy Code: WELCOME10</span>
                <span className={couponCopied ? '' : 'plp-hero-card__btn-label--hidden'}>Copied!</span>
              </span>
            </button>
          </div>
        </div>
      </section>

      {/* ════════════════════════════════════════
          Filter Bar — 分類 Pills（sticky）
          ════════════════════════════════════════ */}
      <div className="plp-filter-bar" role="navigation" aria-label="Product filters">
        <div className="plp-filter-bar__inner">
          <div className="plp-filter-bar__pills" role="list">
            {categories.map((cat) => (
              <button
                key={cat}
                type="button"
                role="listitem"
                className={`plp-filter-pill ${activeFilter === cat ? 'plp-filter-pill--active' : 'plp-filter-pill--inactive'}`}
                onClick={() => setActiveFilter(cat)}
                aria-pressed={activeFilter === cat}
              >
                {cat}
              </button>
            ))}
          </div>
          <div className="plp-filter-bar__right">
            <span className="plp-filter-bar__count">
              {filteredProducts.length} Products Found
            </span>
            <div className="plp-filter-bar__divider" aria-hidden="true" />
            <button type="button" className="plp-filter-bar__sort">
              Sort by: Featured
              <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>expand_more</span>
            </button>
          </div>
        </div>
      </div>

      {/* ════════════════════════════════════════
          商品 Grid
          ════════════════════════════════════════ */}
      <section className="plp-section" aria-label="Product listings">
        <div className="plp-grid" ref={gridRef}>
          {displayedProducts.map((product, index) => {
            const btnState = addingMap[product.id]
            const isSoldOut = product.stock === 0
            const isDisabled = isSoldOut || !!btnState
            /* 第一張卡加 Editor's Pick badge */
            const showBadge = index === 0

            return (
              <article key={product.id} className="plp-card plp-card--reveal">
                {/* 圖片容器：愛心、加入購物車按鈕均在圖片內 */}
                <div className="plp-card__img-wrap">
                  <ProductImage imageUrl={product.imageUrl} name={product.name} />

                  {/* 愛心收藏按鈕（右上）*/}
                  <button
                    type="button"
                    className="plp-card__fav-btn"
                    aria-label={`Save ${product.name} to wishlist`}
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>favorite</span>
                  </button>

                  {/* Editor's Pick badge（左下）*/}
                  {showBadge && (
                    <span className="plp-card__badge">Editor's Pick</span>
                  )}

                  {/* Sold Out overlay */}
                  {isSoldOut && (
                    <div className="plp-card__sold-overlay" aria-hidden="true">
                      <span className="plp-card__sold-tag">Back in Stock Soon</span>
                    </div>
                  )}

                  {/* 加入購物車圓形按鈕（右下，圖片內）*/}
                  <button
                    type="button"
                    className={[
                      'plp-btn',
                      isSoldOut ? 'plp-btn--sold-out' : '',
                      btnState === 'loading' ? 'plp-btn--loading' : '',
                      btnState === 'done' ? 'plp-btn--done' : '',
                    ].filter(Boolean).join(' ')}
                    disabled={isDisabled}
                    onClick={() => handleAddToCart(product.id)}
                    aria-label={isSoldOut ? `${product.name} — Sold Out` : `Add ${product.name} to cart`}
                  >
                    {btnState === 'done' && (
                      <span className="material-symbols-outlined" style={{ fontSize: '20px' }} aria-hidden="true">check</span>
                    )}
                    {btnState === 'loading' && (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true" className="plp-btn__spinner">
                        <circle cx="12" cy="12" r="10" strokeOpacity="0.25" />
                        <path d="M12 2a10 10 0 0 1 10 10" />
                      </svg>
                    )}
                    {!btnState && (
                      <span className="material-symbols-outlined" style={{ fontSize: '20px' }} aria-hidden="true">shopping_bag</span>
                    )}
                  </button>
                </div>

                {/* 名稱、描述、價格 */}
                <div className="plp-card__body">
                  <div className="plp-card__info">
                    <h2 className="plp-card__name" title={product.name}>
                      {product.name}
                    </h2>
                    {product.description && (
                      <p className="plp-card__desc">{product.description}</p>
                    )}
                  </div>
                  <p className={`plp-card__price ${isSoldOut ? 'plp-card__price--sold-out' : ''}`}>
                    NT${Number(product.price).toLocaleString()}
                  </p>
                </div>
              </article>
            )
          })}
        </div>

        {/* ── Explore More ── */}
        {filteredProducts.length > 6 && (
          <div className="plp-explore-more">
            {!showAll && (
              <button
                type="button"
                className="plp-explore-more__btn"
                onClick={() => setShowAll(true)}
              >
                Explore More
              </button>
            )}
            <div className="plp-explore-more__dots" aria-hidden="true">
              <span className={`plp-explore-more__dot${showAll ? '' : ' plp-explore-more__dot--active'}`} />
              <span className={`plp-explore-more__dot${showAll ? ' plp-explore-more__dot--active' : ''}`} />
              <span className="plp-explore-more__dot" />
            </div>
          </div>
        )}
      </section>

      {/* ════════════════════════════════════════
          Material Swatch Feature Section
          ════════════════════════════════════════ */}
      <section className="plp-swatch-section" aria-label="Material swatches">
        <div className="plp-swatch-layout">
          {/* 左：材質特寫圖 */}
          <div className="plp-swatch-img-wrap">
            <img
              className="plp-swatch-img"
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuCe2nyDpEO87YD3ibn1QYtxujaAk1cLaEhCFWkfx7RyZwIXQbHTlEk9XA7f1ZLh_xpKmh1nved0dPT9Qfr-SUOFfMJ3_rEQX89SW6ZO2s2TaVLuZlZ81OTX_PKAvaK08HghcoOYPEkN3ZnJEvY-LcGt9m-8y2smrwrnxiArZghkvl1oEmeYAuxoe8G90L0FvUnFGrM8mPsxqCTYt2sYj8wTpV141uz_iK_3vgTlCiO4qESwZlvF6eZMnulfmH0On-cJofrQiRPTFBdd"
              alt="FurnitureCo material swatches — linen, brushed gold, white oak"
            />
          </div>

          {/* 右：文案 + 色票 + 按鈕 */}
          <div className="plp-swatch-content">
            <span className="plp-swatch-eyebrow">The FurnitureCo Standards</span>
            <h2 className="plp-swatch-title">
              Tactile Excellence in<br />
              <em>Every Fiber</em>
            </h2>
            <p className="plp-swatch-desc">
              We believe your home should be as pleasant to touch as it is to see. Our
              materials are sourced from sustainable forests and master weavers across Europe.
            </p>

            <div className="plp-swatches">
              <p className="plp-swatches__label">Explore Our Swatches</p>
              <div className="plp-swatches__row">
                <div className="plp-swatch-item plp-swatch-item--active">
                  <div className="plp-swatch-circle" style={{ background: '#E5E0D8' }} />
                  <span className="plp-swatch-name">Sand Linen</span>
                </div>
                <div className="plp-swatch-item">
                  <div className="plp-swatch-circle" style={{ background: '#5C5144' }} />
                  <span className="plp-swatch-name">Dark Oak</span>
                </div>
                <div className="plp-swatch-item">
                  <div className="plp-swatch-circle" style={{ background: '#B8A99A' }} />
                  <span className="plp-swatch-name">Greige Wool</span>
                </div>
              </div>
            </div>

            <button type="button" className="plp-swatch-order-btn">
              Order Fabric Samples
            </button>
          </div>
        </div>
      </section>

      {/* ════════════════════════════════════════
          Footer
          ════════════════════════════════════════ */}
      <footer className="plp-footer" aria-label="Site footer">
        <div className="plp-footer__inner">
          {/* 品牌欄 */}
          <div className="plp-footer__brand">
            <span className="plp-footer__brand-name">FurnitureCo.</span>
            <p className="plp-footer__brand-desc">
              Creating spaces of warmth, serenity, and functional beauty since 2012.
            </p>
            <div className="plp-footer__socials">
              <span className="material-symbols-outlined" style={{ fontSize: '22px' }}>public</span>
              <span className="material-symbols-outlined" style={{ fontSize: '22px' }}>potted_plant</span>
              <span className="material-symbols-outlined" style={{ fontSize: '22px' }}>chair</span>
            </div>
          </div>

          {/* Shopping 欄 */}
          <div className="plp-footer__col">
            <p className="plp-footer__col-title">Shopping</p>
            <ul className="plp-footer__links">
              <li><a className="plp-footer__link" href="#">Store Locator</a></li>
              <li><a className="plp-footer__link" href="#">Furniture Care</a></li>
              <li><a className="plp-footer__link" href="#">Trade Program</a></li>
              <li><a className="plp-footer__link" href="#">Financing</a></li>
            </ul>
          </div>

          {/* Support 欄 */}
          <div className="plp-footer__col">
            <p className="plp-footer__col-title">Support</p>
            <ul className="plp-footer__links">
              <li><a className="plp-footer__link" href="#">Shipping &amp; Returns</a></li>
              <li><a className="plp-footer__link" href="#">Privacy Policy</a></li>
              <li><a className="plp-footer__link" href="#">Terms of Service</a></li>
              <li><a className="plp-footer__link" href="#">Contact Us</a></li>
            </ul>
          </div>

          {/* Stay Inspired 欄 */}
          <div className="plp-footer__newsletter">
            <p className="plp-footer__col-title">Stay Inspired</p>
            <p className="plp-footer__newsletter-desc">Sign up for design tips and new arrivals.</p>
            <form
              className="plp-footer__newsletter-form"
              onSubmit={(e) => e.preventDefault()}
              aria-label="Newsletter signup"
            >
              <input
                type="email"
                className="plp-footer__newsletter-input"
                placeholder="Email Address"
                aria-label="Email address for newsletter"
              />
              <button type="submit" className="plp-footer__newsletter-btn" aria-label="Subscribe">
                <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>arrow_forward</span>
              </button>
            </form>
          </div>
        </div>

        {/* 底部版權列 */}
        <div className="plp-footer__bottom">
          <p className="plp-footer__copyright">© 2024 FurnitureCo. Designed for Hygge living.</p>
          <div className="plp-footer__payment">
            <span>VISA</span>
            <span>MASTERCARD</span>
            <span>APPLE PAY</span>
          </div>
        </div>
      </footer>
    </main>
  )
}

/**
 * 商品圖片元件。
 *
 * 若 imageUrl 有值則渲染 img 標籤；
 * 若 imageUrl 為空或圖片載入失敗（onError），則渲染暖色佔位區塊。
 *
 * @param {{ imageUrl: string|null|undefined, name: string }} props
 */
function ProductImage({ imageUrl, name }) {
  /** @type {[boolean, Function]} 圖片是否發生載入錯誤 */
  const [imgError, setImgError] = useState(false)

  if (!imageUrl || imgError) {
    return (
      <div className="plp-card__img-placeholder" role="img" aria-label={`${name} — no image`}>
        {/* 暖色 SVG 佔位圖示 */}
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#c8a882" strokeWidth="1" aria-hidden="true">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
    )
  }

  return (
    <img
      className="plp-card__img"
      src={imageUrl}
      alt={name}
      onError={() => setImgError(true)}
    />
  )
}
