import { createContext, useCallback, useContext, useState } from 'react'
import { addToCart, checkout, getCart, removeCartItem, updateCartItem } from '../api/cart'

/**
 * 購物車 React Context。
 * 內部使用，外部請透過 useCart hook 存取。
 */
const CartContext = createContext(null)

/**
 * 購物車 Context Provider。
 *
 * 將此元件包在應用程式根層，子元件即可透過 useCart() 存取購物車狀態與操作。
 *
 * 提供以下 value：
 * - cart           {Object|null}  購物車資料（初始為 null，尚未載入）
 * - totalQuantity  {number}       購物車總件數（供導覽列徽章顯示）
 * - refreshCart()                 重新從 API 取得購物車並更新 state
 * - addItem(productId, quantity)  加入商品後自動 refresh
 * - updateItem(itemId, quantity)  更新數量後自動 refresh
 * - removeItem(itemId)            移除明細後自動 refresh
 * - doCheckout(recipient)         結帳後自動 refresh
 *
 * @param {{ children: React.ReactNode }} props
 */
export function CartProvider({ children }) {
  /** @type {[Object|null, Function]} 購物車資料，初始為 null */
  const [cart, setCart] = useState(null)

  /**
   * 重新從 API 取得購物車並更新 state。
   * 各操作函式呼叫完 API 後會自動呼叫此函式保持資料同步。
   */
  const refreshCart = useCallback(async () => {
    const data = await getCart()
    setCart(data)
  }, [])

  /**
   * 將商品加入購物車，完成後自動更新購物車 state。
   *
   * @param {string} productId - 商品 UUID
   * @param {number} [quantity=1] - 加入數量
   */
  const addItem = useCallback(async (productId, quantity = 1) => {
    await addToCart(productId, quantity)
    await refreshCart()
  }, [refreshCart])

  /**
   * 更新購物車明細數量，完成後自動更新購物車 state。
   * quantity 為 0 時伺服器會自動刪除該明細。
   *
   * @param {string} itemId - 購物車明細 UUID
   * @param {number} quantity - 新數量（0 表示刪除）
   */
  const updateItem = useCallback(async (itemId, quantity) => {
    await updateCartItem(itemId, quantity)
    await refreshCart()
  }, [refreshCart])

  /**
   * 移除購物車中的指定明細，完成後自動更新購物車 state。
   *
   * @param {string} itemId - 購物車明細 UUID
   */
  const removeItem = useCallback(async (itemId) => {
    await removeCartItem(itemId)
    await refreshCart()
  }, [refreshCart])

  /**
   * 送出結帳，完成後自動更新購物車 state（購物車會被清空）。
   *
   * @param {{ name: string, phone: string, email: string, address: string }} recipient - 收件人資料
   * @returns {Promise<Object>} 訂單摘要
   */
  const doCheckout = useCallback(async (recipient) => {
    const result = await checkout(recipient)
    await refreshCart()
    return result
  }, [refreshCart])

  /** 總件數，直接從 cart 衍生；cart 為 null 時為 0 */
  const totalQuantity = cart?.totalQuantity ?? 0

  return (
    <CartContext.Provider value={{ cart, totalQuantity, refreshCart, addItem, updateItem, removeItem, doCheckout }}>
      {children}
    </CartContext.Provider>
  )
}

/**
 * 取得購物車 context 的自訂 hook。
 *
 * 必須在 CartProvider 內部的元件中使用，否則會拋出錯誤。
 *
 * @returns {{ cart: Object|null, totalQuantity: number, refreshCart: Function, addItem: Function, updateItem: Function, removeItem: Function, doCheckout: Function }}
 */
export function useCart() {
  const context = useContext(CartContext)
  if (context === null) {
    throw new Error('useCart 必須在 CartProvider 內部使用')
  }
  return context
}
