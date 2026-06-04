import { test, expect, Page } from '@playwright/test'

/**
 * FurnitureCo. 購物車端對端測試。
 * 涵蓋完整購物流程：瀏覽商品 → 加入購物車 → 調整數量 → 結帳 → 成功頁。
 * 每個 test 共用同一個 browser context（session cookie 持續）。
 */

// ── 工具函式 ───────────────────────────────────────────────────────────────

/** 等待商品列表載入完成（至少出現一張商品卡片）。 */
async function waitForProducts(page: Page) {
  await expect(page.locator('.plp-card').first()).toBeVisible({ timeout: 10_000 })
}

// ── 測試案例 ───────────────────────────────────────────────────────────────

test.describe('FurnitureCo. 購物流程', () => {

  test('1. 商品列表頁正確顯示 5 項商品', async ({ page }) => {
    await page.goto('/')
    await waitForProducts(page)

    const cards = page.locator('.plp-card')
    await expect(cards).toHaveCount(5)

    // 確認頁面標題
    await expect(page.locator('.plp-title')).toHaveText('Products')
  })

  test('2. NavBar 顯示 FurnitureCo. 品牌名稱', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.navbar__logo')).toHaveText('FurnitureCo.')
  })

  test('3. 加入有庫存的商品後，NavBar 徽章更新', async ({ page }) => {
    await page.goto('/')
    await waitForProducts(page)

    // 取得第一張非售完的卡片
    const firstAvailableBtn = page.locator('.plp-btn:not(.plp-btn--sold-out)').first()
    await firstAvailableBtn.click()
    await expect(firstAvailableBtn).toHaveText(/✓ Added/, { timeout: 5_000 })

    // 徽章應顯示 1
    await expect(page.locator('.navbar__badge')).toBeVisible()
    await expect(page.locator('.navbar__badge')).toHaveText('1')
  })

  test('4. 購物車頁顯示已加入的商品', async ({ page }) => {
    await page.goto('/')
    await waitForProducts(page)

    // 加入第一張可購買商品
    const firstAvailableBtn = page.locator('.plp-btn:not(.plp-btn--sold-out)').first()
    await firstAvailableBtn.click()
    await expect(firstAvailableBtn).toHaveText(/✓ Added/, { timeout: 5_000 })

    // 前往購物車
    await page.locator('.navbar__cart-link').click()
    await expect(page).toHaveURL('/cart')

    // 購物車有明細
    await expect(page.locator('.cart-item-row')).toHaveCount(1)
  })

  test('5. 購物車頁可調整數量（+1）', async ({ page }) => {
    await page.goto('/')
    await waitForProducts(page)

    await page.locator('.plp-btn:not(.plp-btn--sold-out)').first().click()
    await page.locator('.navbar__cart-link').click()
    await expect(page).toHaveURL('/cart')
    await expect(page.locator('.cart-item-row')).toHaveCount(1)

    // 初始數量為 1，點 + 後變 2
    const plusBtn = page.locator('.qty-btn').nth(1) // 第 1 個是 −，第 2 個是 +
    await plusBtn.click()

    await expect(page.locator('.qty-value')).toHaveText('2', { timeout: 5_000 })
  })

  test('6. 購物車頁可減少數量（數量歸零自動移除）', async ({ page }) => {
    await page.goto('/')
    await waitForProducts(page)

    await page.locator('.plp-btn:not(.plp-btn--sold-out)').first().click()
    await page.locator('.navbar__cart-link').click()
    await expect(page.locator('.cart-item-row')).toHaveCount(1)

    // 點 − 讓數量歸 0 → 應自動移除，顯示空購物車
    const minusBtn = page.locator('.qty-btn').first()
    await minusBtn.click()

    await expect(page.locator('.cart-empty-msg')).toBeVisible({ timeout: 5_000 })
  })

  test('7. 售完商品按鈕顯示 Sold Out 且不可點擊', async ({ page }) => {
    await page.goto('/')
    await waitForProducts(page)

    const soldOutBtn = page.locator('.plp-btn--sold-out').first()
    await expect(soldOutBtn).toBeVisible()
    await expect(soldOutBtn).toHaveText('Sold Out')
    await expect(soldOutBtn).toBeDisabled()
  })

  test('8. 完整結帳流程：加入 → 購物車 → 填表單 → 成功頁', async ({ page }) => {
    await page.goto('/')
    await waitForProducts(page)

    // 加入第一個可購買商品
    await page.locator('.plp-btn:not(.plp-btn--sold-out)').first().click()
    await expect(page.locator('.plp-btn:not(.plp-btn--sold-out)').first()).toHaveText(/✓ Added/, { timeout: 5_000 })

    // 前往購物車並結帳
    await page.locator('.navbar__cart-link').click()
    await expect(page.locator('.cart-item-row')).toHaveCount(1)
    await page.locator('.cart-checkout-btn').click()
    await expect(page).toHaveURL('/checkout')

    // 填寫收件資料
    await page.locator('input[name="name"]').fill('Test User')
    await page.locator('input[name="phone"]').fill('0912345678')
    await page.locator('input[name="email"]').fill('test@example.com')
    await page.locator('input[name="address"]').fill('123 Main Street, Taipei')

    // 送出訂單
    await page.locator('.checkout-submit-btn').click()

    // 確認成功頁
    await expect(page).toHaveURL('/checkout/success', { timeout: 10_000 })
    await expect(page.locator('.success-title')).toHaveText('Order Confirmed!')
    await expect(page.locator('.success-items tbody tr')).toHaveCount(1)

    // 結帳後徽章應歸零（購物車清空）
    await expect(page.locator('.navbar__badge')).not.toBeVisible()
  })

})
