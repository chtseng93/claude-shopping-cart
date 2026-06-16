"""
Demo 錄影腳本：慢速購物流程，加入 4 個商品 → 購物車 → 結帳 → 套用優惠券 → 送出
輸出：scripts/demo_video/*.webm
"""
from playwright.sync_api import sync_playwright
import os, time

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "demo_video")
os.makedirs(OUTPUT_DIR, exist_ok=True)

PRODUCTS = [
    "Linen Dining Chair",
    "Velvet Ottoman",
]

with sync_playwright() as p:
    browser = p.chromium.launch(headless=False, slow_mo=700)
    context = browser.new_context(
        viewport={"width": 1280, "height": 720},
        record_video_dir=OUTPUT_DIR,
        record_video_size={"width": 1280, "height": 720},
    )
    page = context.new_page()

    # --- 首頁：瀏覽並逐一加入商品 ---
    page.goto("http://localhost:5173")
    page.wait_for_load_state("networkidle")
    time.sleep(2)

    for name in PRODUCTS:
        btn = page.locator(f'button[aria-label="Add {name} to cart"]')
        # 先捲到商品卡片，停頓讓觀看者看到商品
        btn.scroll_into_view_if_needed()
        time.sleep(2.5)
        btn.click()
        time.sleep(2)

    time.sleep(2)

    # --- 前往購物車 ---
    page.locator("a[href='/cart']").click()
    page.wait_for_load_state("networkidle")
    time.sleep(2.5)

    # --- 往下捲讓觀看者看清楚購物車商品，再點 Checkout ---
    page.evaluate("window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'})")
    time.sleep(2.5)
    page.locator("button", has_text="Checkout").click()
    page.wait_for_load_state("networkidle")
    time.sleep(1.5)

    # --- 填寫表單 ---
    page.locator("input[name='name']").fill("Alex Chen")
    time.sleep(0.5)
    page.locator("input[name='phone']").fill("0912345678")
    time.sleep(0.5)
    page.locator("input[type='email']").fill("alex@example.com")
    time.sleep(0.5)
    page.locator("input[name='address']").fill("123 Main Street, Taipei")
    time.sleep(1)

    # --- 套用優惠券 ---
    page.locator("input[placeholder='Enter coupon code']").fill("WELCOME10")
    time.sleep(1.5)
    page.locator("button", has_text="Apply").click()
    time.sleep(3)
    # 往下捲讓 Place Order 按鈕進入畫面
    page.evaluate("window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'})")
    time.sleep(2)

    # --- 送出訂單 ---
    page.locator("button", has_text="Place Order").click()
    page.wait_for_load_state("networkidle")
    time.sleep(2.5)
    # 往上捲顯示綠色成功勾勾
    page.evaluate("window.scrollTo({top: 0, behavior: 'smooth'})")
    time.sleep(4)

    context.close()
    browser.close()

webm_files = [f for f in os.listdir(OUTPUT_DIR) if f.endswith(".webm")]
if webm_files:
    webm_path = os.path.join(OUTPUT_DIR, webm_files[0])
    print(f"錄影完成：{webm_path}")
    print(f"檔案大小：{os.path.getsize(webm_path) // 1024} KB")
else:
    print("找不到錄影檔")
