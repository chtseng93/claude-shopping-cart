import { BrowserRouter, Route, Routes } from 'react-router-dom'
import NavBar from './components/NavBar'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import CheckoutSuccessPage from './pages/CheckoutSuccessPage'
import ProductListPage from './pages/ProductListPage'
import './App.css'

/**
 * 應用程式根元件（App）。
 *
 * 以 BrowserRouter 包裹全站路由，並在每個頁面上方顯示共用 NavBar。
 *
 * 路由對照：
 * - /                  → ProductListPage（商品列表頁）
 * - /cart              → CartPage（購物車頁）
 * - /checkout          → CheckoutPage（結帳頁）
 * - /checkout/success  → CheckoutSuccessPage（結帳成功頁）
 */
function App() {
  return (
    <BrowserRouter>
      {/* ── 全站共用導覽列 ── */}
      <NavBar />

      {/* ── 頁面路由 ── */}
      <Routes>
        {/* 商品列表頁（首頁） */}
        <Route path="/" element={<ProductListPage />} />

        {/* 購物車頁 */}
        <Route path="/cart" element={<CartPage />} />

        {/* 結帳頁 */}
        <Route path="/checkout" element={<CheckoutPage />} />

        {/* 結帳成功頁 */}
        <Route path="/checkout/success" element={<CheckoutSuccessPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
