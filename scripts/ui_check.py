"""快速截圖驗證各頁面 UI 效果"""
from playwright.sync_api import sync_playwright
import os, time

SCREENSHOTS = [
    ("http://localhost:5173/", "01_product_list.png"),
    ("http://localhost:5173/cart", "02_cart.png"),
    ("http://localhost:5173/checkout", "03_checkout.png"),
]

OUT = "scripts/screenshots"
os.makedirs(OUT, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1280, "height": 900})

    for url, name in SCREENSHOTS:
        page.goto(url)
        page.wait_for_load_state("networkidle")
        time.sleep(0.5)
        path = os.path.join(OUT, name)
        page.screenshot(path=path, full_page=True)
        print(f"Saved: {path}")

    browser.close()
    print("Done.")
