import { test, expect } from '@playwright/test'

/**
 * FurnitureCo. Demo 影片測試。
 * 完整展示：瀏覽商品 → 加入購物車 → 套用 WELCOME10 優惠券 → 結帳成功。
 * 節奏刻意放慢，供錄製 README demo 影片使用。
 */
test('FurnitureCo. 完整購物與優惠券結帳流程', async ({ page }) => {

  // ── 場景 1：首頁 & 新會員橫幅 ───────────────────────────────────────────

  await page.goto('/')
  // 等商品載入
  await expect(page.locator('.plp-card').first()).toBeVisible({ timeout: 10_000 })
  // 讓鏡頭停在首頁橫幅上欣賞一秒
  await page.waitForTimeout(1500)

  // 捲動讓 Banner 完整入鏡
  const banner = page.locator('.new-member-banner')
  await banner.scrollIntoViewIfNeeded()
  await page.waitForTimeout(1000)

  // ── 場景 2：選商品加入購物車 ────────────────────────────────────────────

  // 捲回頂端，看商品列表
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  await page.waitForTimeout(800)

  // 點擊第一個有庫存商品的 Add to Cart 按鈕
  const addBtn = page.locator('.plp-btn:not(.plp-btn--sold-out)').first()
  await addBtn.scrollIntoViewIfNeeded()
  await page.waitForTimeout(500)
  await addBtn.click()

  // 等待按鈕顯示 ✓ Added（Toast 動畫也會同時出現）
  await expect(addBtn).toHaveText(/✓ Added/, { timeout: 5_000 })
  await page.waitForTimeout(1500)

  // ── 場景 3：前往購物車 ───────────────────────────────────────────────────

  await page.locator('.navbar__cart-link').click()
  await expect(page).toHaveURL('/cart')
  await expect(page.locator('.cart-item-row')).toHaveCount(1)
  await page.waitForTimeout(1200)

  // ── 場景 4：進入結帳頁 ───────────────────────────────────────────────────

  await page.locator('.cart-checkout-btn').click()
  await expect(page).toHaveURL('/checkout')
  await page.waitForTimeout(800)

  // ── 場景 5：填寫收件資料 ─────────────────────────────────────────────────

  await page.locator('input[name="name"]').fill('Olivia Chen')
  await page.waitForTimeout(300)
  await page.locator('input[name="phone"]').fill('0912345678')
  await page.waitForTimeout(300)
  await page.locator('input[name="email"]').fill('olivia@example.com')
  await page.waitForTimeout(300)
  await page.locator('input[name="address"]').fill('12 Xinyi Road, Taipei')
  await page.waitForTimeout(600)

  // ── 場景 6：套用 WELCOME10 優惠券 ────────────────────────────────────────

  // 捲動到優惠券輸入區
  const couponSection = page.locator('.coupon-input')
  await couponSection.scrollIntoViewIfNeeded()
  await page.waitForTimeout(800)

  // 輸入優惠券代碼
  const couponInput = page.locator('.coupon-input__field')
  await couponInput.click()
  await couponInput.fill('WELCOME10')
  await page.waitForTimeout(500)

  // 點擊 Apply
  await page.locator('.coupon-input__apply-btn').click()

  // 等待折扣結果顯示
  await expect(page.locator('.coupon-input__result')).toBeVisible({ timeout: 5_000 })
  await page.waitForTimeout(1500)

  // ── 場景 7：送出訂單 ─────────────────────────────────────────────────────

  // 捲到送出按鈕
  const submitBtn = page.locator('.checkout-submit-btn')
  await submitBtn.scrollIntoViewIfNeeded()
  await page.waitForTimeout(600)
  await submitBtn.click()

  // ── 場景 8：成功頁 ───────────────────────────────────────────────────────

  await expect(page).toHaveURL('/checkout/success', { timeout: 10_000 })
  await expect(page.locator('.success-title')).toHaveText('Order Confirmed!')
  // 停在成功頁讓觀眾看清楚
  await page.waitForTimeout(2500)
})
