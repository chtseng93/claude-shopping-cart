import { test, expect } from '@playwright/test'

/**
 * FurnitureCo. Demo 影片錄製腳本 v2（UI 重設計版）。
 * 完整展示：Hero Banner → 選兩件商品加入購物車 → 套用 WELCOME10 → 結帳成功。
 * 搭配 playwright-demo2.config.ts 使用。
 */
test('FurnitureCo. 完整購物流程 Demo v2', async ({ page }) => {

  // ── 場景 1：首頁 Hero Banner ─────────────────────────────────────────────
  await page.goto('/')
  await expect(page.locator('.plp-card').first()).toBeVisible({ timeout: 10_000 })

  // 停在 Banner 讓影片跑完（Ken Burns + 文字進場動畫約 3 秒）
  await page.waitForTimeout(4000)

  // ── 場景 1b：點擊 Hero Card 複製優惠券 ──────────────────────────────────
  const copyBtn = page.locator('.plp-hero-card__btn')
  await copyBtn.scrollIntoViewIfNeeded()
  await page.waitForTimeout(500)
  await copyBtn.click()
  // 等待按鈕變成 Copied! 狀態（金色 + check icon）
  await expect(copyBtn).toHaveClass(/plp-hero-card__btn--copied/, { timeout: 3_000 })
  await page.waitForTimeout(1500)

  // ── 場景 2：捲動到商品區 ─────────────────────────────────────────────────
  await page.evaluate(() => window.scrollTo({ top: 650, behavior: 'smooth' }))
  await page.waitForTimeout(1200)

  // ── 場景 3：加入第一件商品 ──────────────────────────────────────────────
  const availableBtns = page.locator('.plp-btn:not(.plp-btn--sold-out)')

  const firstBtn = availableBtns.nth(0)
  await firstBtn.scrollIntoViewIfNeeded()
  await page.waitForTimeout(500)
  await firstBtn.click()
  await expect(firstBtn).toHaveClass(/plp-btn--done/, { timeout: 5_000 })
  await page.waitForTimeout(1500)

  // ── 場景 4：加入第二件商品 ──────────────────────────────────────────────
  const secondBtn = availableBtns.nth(1)
  await secondBtn.scrollIntoViewIfNeeded()
  await page.waitForTimeout(600)
  await secondBtn.click()
  await expect(secondBtn).toHaveClass(/plp-btn--done/, { timeout: 5_000 })
  // 讓 Toast + 徽章顯示 2 的動畫跑完
  await page.waitForTimeout(1800)

  // ── 場景 5：捲回頂部，點擊 NavBar 購物車 ────────────────────────────────
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  await page.waitForTimeout(700)

  // 停在購物車圖示上讓觀眾看到徽章
  await page.locator('.navbar__cart-link').hover()
  await page.waitForTimeout(1200)
  await page.locator('.navbar__cart-link').click()
  await expect(page).toHaveURL('/cart')
  await expect(page.locator('.cart-item-row')).toHaveCount(2)
  await page.waitForTimeout(1200)

  // ── 場景 6：前往結帳頁 ───────────────────────────────────────────────────
  await page.locator('.cart-checkout-btn').click()
  await expect(page).toHaveURL('/checkout')
  // 停頓讓觀眾看清楚結帳頁面
  await page.waitForTimeout(2000)

  // ── 場景 7：填寫收件資料 ─────────────────────────────────────────────────
  await page.locator('input[name="name"]').fill('Olivia Chen')
  await page.waitForTimeout(300)
  await page.locator('input[name="phone"]').fill('0912345678')
  await page.waitForTimeout(300)
  await page.locator('input[name="email"]').fill('olivia@example.com')
  await page.waitForTimeout(300)
  await page.locator('input[name="address"]').fill('12 Xinyi Road, Taipei')
  await page.waitForTimeout(700)

  // ── 場景 8：套用 WELCOME10 優惠券 ────────────────────────────────────────
  const couponSection = page.locator('.coupon-input')
  await couponSection.scrollIntoViewIfNeeded()
  await page.waitForTimeout(700)

  const couponInput = page.locator('.coupon-input__field')
  await couponInput.click()
  await couponInput.fill('WELCOME10')
  await page.waitForTimeout(500)

  await page.locator('.coupon-input__apply-btn').click()
  await expect(page.locator('.coupon-input__result')).toBeVisible({ timeout: 5_000 })
  await page.waitForTimeout(1000)

  // 往下滑讓訂單總金額與折扣完整入鏡
  await page.evaluate(() => window.scrollBy({ top: 300, behavior: 'smooth' }))
  await page.waitForTimeout(1500)

  // ── 場景 9：送出訂單 ─────────────────────────────────────────────────────
  const submitBtn = page.locator('.checkout-submit-btn')
  await submitBtn.scrollIntoViewIfNeeded()
  await page.waitForTimeout(600)
  await submitBtn.click()

  // ── 場景 10：成功頁 — 往下滑看明細，再往上滑 ────────────────────────────
  await expect(page).toHaveURL('/checkout/success', { timeout: 10_000 })
  await expect(page.locator('.success-title')).toHaveText('Order Confirmed!')
  // 停在頂部讓大標完整呈現
  await page.waitForTimeout(1500)

  // 往下滑看訂單明細
  await page.evaluate(() => window.scrollTo({ top: 500, behavior: 'smooth' }))
  await page.waitForTimeout(1500)

  // 往上滑回頂部
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  await page.waitForTimeout(1500)
})
