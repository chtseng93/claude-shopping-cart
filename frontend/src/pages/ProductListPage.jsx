import { useEffect, useState } from 'react'
import { getProducts } from '../api/products'
import { useCart } from '../context/CartContext'
import NewMemberBanner from '../components/NewMemberBanner'
import './ProductListPage.css'

/**
 * 商品列表頁（T11）。
 *
 * 頁面掛載時呼叫 getProducts() 取得所有商品，
 * 以卡片 Grid 排列顯示商品資訊（圖片、名稱、價格、庫存），
 * 並提供「加入購物車」功能。
 *
 * 狀態說明：
 * - products      {Array}   商品列表（由 API 取得）
 * - loading       {boolean} 頁面初始載入中
 * - error         {string|null} 頁面初始載入失敗訊息
 * - addingMap     {Object}  key 為 productId，value 為 'loading' | 'done' | undefined
 *                           用於追蹤各按鈕的加入狀態（防連點 + 成功回饋）
 */
export default function ProductListPage() {
  /** @type {[Array, Function]} 商品列表 */
  const [products, setProducts] = useState([])

  /** @type {[boolean, Function]} 頁面初始載入狀態 */
  const [loading, setLoading] = useState(true)

  /** @type {[string|null, Function]} 初始載入錯誤訊息 */
  const [error, setError] = useState(null)

  /**
   * 各商品加入購物車的按鈕狀態對照表。
   * key 為 productId，value 為 'loading'（請求中）| 'done'（成功 1.5s）| undefined（正常）
   *
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

  /**
   * 點擊「加入購物車」按鈕的處理函式。
   *
   * 流程：
   * 1. 設定該商品按鈕為 loading（防止連點）
   * 2. 呼叫 addItem(productId, 1)
   * 3. 成功 → 設定按鈕為 done（顯示「✓ 已加入」1.5 秒）後恢復正常
   * 4. 失敗 → alert 顯示錯誤訊息，按鈕立即恢復正常
   *
   * @param {string} productId - 商品 UUID
   */
  async function handleAddToCart(productId) {
    /* 若已在 loading 或 done 狀態則不重複觸發 */
    if (addingMap[productId]) return

    setAddingMap((prev) => ({ ...prev, [productId]: 'loading' }))

    try {
      await addItem(productId, 1)
      setAddingMap((prev) => ({ ...prev, [productId]: 'done' }))

      /* 1.5 秒後恢復按鈕為正常狀態 */
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

  /**
   * 根據商品庫存與按鈕狀態，決定按鈕文字。
   *
   * @param {Object} product - 商品物件
   * @returns {string} 按鈕文字
   */
  function getButtonLabel(product) {
    if (product.stock === 0) return 'Sold Out'
    const state = addingMap[product.id]
    if (state === 'loading') return 'Adding...'
    if (state === 'done') return '✓ Added'
    return 'Add to Cart'
  }

  /* ── 頁面初始載入中 ── */
  if (loading) {
    return (
      <main className="plp-container">
        <p className="plp-status">Loading products...</p>
      </main>
    )
  }

  /* ── 初始載入失敗 ── */
  if (error) {
    return (
      <main className="plp-container">
        <p className="plp-status plp-status--error">Failed to load: {error}</p>
      </main>
    )
  }

  /* ── 正常渲染 ── */
  return (
    <main className="plp-container">
      {/* 新會員折扣碼提示橫幅 */}
      <NewMemberBanner />

      {/* Hero 區塊：左側大標題 + 垂直分隔線 + 右側季節特輯 */}
      <div className="plp-hero">
        <div className="plp-hero__content">
          <span className="plp-hero__accent" aria-hidden="true" />
          <h1 className="plp-hero__title">Products</h1>
          <p className="plp-hero__subtitle">
            Timeless design. Quality craftsmanship.<br />
            Made for modern living.
          </p>
        </div>

        <div className="plp-hero__vline" aria-hidden="true" />

        <div className="plp-hero__panel">
          <span className="plp-hero__panel-label">New Season Edit</span>
          <h2 className="plp-hero__panel-title">Autumn Collection</h2>
          <p className="plp-hero__panel-text">
            Warm textures. Natural materials.<br />
            Thoughtfully designed for the way you live.
          </p>
          <span className="plp-hero__panel-rule" aria-hidden="true" />
          <a href="#plp-grid" className="plp-hero__panel-cta">
            Explore collection <span aria-hidden="true">→</span>
          </a>
          <p className="plp-hero__panel-tagline" aria-hidden="true">
            <span className="plp-hero__panel-tagline-line" />
            Curated for calm interiors
            <span className="plp-hero__panel-tagline-line" />
          </p>
        </div>
      </div>

      <div id="plp-grid" className="plp-grid">
        {products.map((product) => {
          const btnState = addingMap[product.id]
          const isSoldOut = product.stock === 0
          const isDisabled = isSoldOut || !!btnState

          return (
            <article key={product.id} className="plp-card">
              {/* 商品圖片：有值則顯示 img，onError 時改為佔位區塊 */}
              <ProductImage imageUrl={product.imageUrl} name={product.name} />

              <div className="plp-card__body">
                <h2 className="plp-card__name" title={product.name}>
                  {product.name}
                </h2>
                {product.description && (
                  <p className="plp-card__desc">{product.description}</p>
                )}
                {/* 琥珀色分隔線 */}
                <span className="plp-card__divider" aria-hidden="true" />
                <p className="plp-card__price">
                  NT${Number(product.price).toLocaleString()}
                </p>
                <p className={`plp-card__stock ${isSoldOut ? 'plp-card__stock--sold-out' : ''}`}>
                  {isSoldOut ? 'Sold Out' : `In Stock: ${product.stock}`}
                </p>
              </div>

              <div className="plp-card__footer">
                <button
                  type="button"
                  className={`plp-btn ${isSoldOut ? 'plp-btn--sold-out' : ''} ${btnState === 'done' ? 'plp-btn--done' : ''}`}
                  disabled={isDisabled}
                  onClick={() => handleAddToCart(product.id)}
                  aria-label={isSoldOut ? `${product.name} — Sold Out` : `Add ${product.name} to cart`}
                >
                  {/* 購物車圖示：僅在正常狀態顯示 */}
                  {!isSoldOut && !btnState && (
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <circle cx="9" cy="21" r="1"/>
                      <circle cx="20" cy="21" r="1"/>
                      <path d="M1 1h4l2.68 13.39a2 2 0 001.99 1.61h9.72a2 2 0 001.98-1.69l1.6-9.31H6"/>
                    </svg>
                  )}
                  {getButtonLabel(product)}
                </button>
              </div>
            </article>
          )
        })}
      </div>
    </main>
  )
}

/**
 * 商品圖片元件。
 *
 * 若 imageUrl 有值則渲染 img 標籤；
 * 若 imageUrl 為空或圖片載入失敗（onError），則渲染灰色佔位區塊。
 *
 * @param {{ imageUrl: string|null|undefined, name: string }} props
 */
function ProductImage({ imageUrl, name }) {
  /** @type {[boolean, Function]} 圖片是否發生載入錯誤 */
  const [imgError, setImgError] = useState(false)

  /* imageUrl 無值，或上次載入失敗 → 顯示佔位區塊 */
  if (!imageUrl || imgError) {
    return <div className="plp-card__img-placeholder" aria-hidden="true" role="img" aria-label="No image" />
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
