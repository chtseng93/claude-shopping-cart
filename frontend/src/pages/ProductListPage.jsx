import { useEffect, useMemo, useState } from 'react'
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
              {/* 疊層：兩段文字佔同一格，保留最寬尺寸 */}
              <span className="plp-hero-card__btn-label">
                <span aria-hidden={couponCopied} className={couponCopied ? 'plp-hero-card__btn-label--hidden' : ''}>Copy Code: WELCOME10</span>
                <span aria-hidden={!couponCopied} className={couponCopied ? '' : 'plp-hero-card__btn-label--hidden'}>Copied!</span>
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
          <span className="plp-filter-bar__count">
            {filteredProducts.length} {filteredProducts.length === 1 ? 'product' : 'products'}
          </span>
        </div>
      </div>

      {/* ════════════════════════════════════════
          商品 Grid
          ════════════════════════════════════════ */}
      <section className="plp-section" aria-label="Product listings">
        <div className="plp-grid">
          {filteredProducts.map((product, index) => {
            const btnState = addingMap[product.id]
            const isSoldOut = product.stock === 0
            const isDisabled = isSoldOut || !!btnState
            /* 第一張卡加 Editor's Pick badge */
            const showBadge = index === 0

            return (
              <article key={product.id} className="plp-card">
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
      </section>
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
