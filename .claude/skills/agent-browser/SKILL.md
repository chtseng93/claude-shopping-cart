---
name: agent-browser-checkout
description: 使用 agent-browser 操作購物車專案的結帳流程（http://localhost:5173）。當使用者要求用 agent-browser 驗證本專案購物流程時觸發。
triggers:
  - /agent-browser-checkout
---

# 購物車專案 agent-browser 結帳流程

> 本專案前端：`http://localhost:5173`（Vite dev server）
> 後端 API：`http://localhost:8083`（Spring Boot）
> 通用 CLI 指令請參考使用者層級 skill：`/agent-browser`

---

## 完整結帳操作流程

```bash
# 1. 開啟頁面（有頭模式）
agent-browser close
agent-browser --headed open http://localhost:5173/

# 2. 取得快照，找到商品加入購物車按鈕
agent-browser snapshot
agent-browser click @e12   # 「Add to Cart」按鈕（snapshot 後確認 ref）

# 3. 點擊導覽列購物車連結（React Router 跳轉，勿用 open /cart）
agent-browser snapshot
agent-browser click @e4    # NavBar 的「Cart」連結

# 4. 調整數量
agent-browser snapshot
agent-browser click @e8    # 「+」按鈕

# 5. 前往結帳頁
agent-browser snapshot
agent-browser click @e6    # 「Proceed to Checkout」按鈕

# 6. 填寫收件資料（React 受控元件必須用 fill）
agent-browser snapshot
agent-browser fill @e3 "王小明"
agent-browser fill @e4 "0912345678"
agent-browser fill @e5 "test@example.com"
agent-browser fill @e6 "台北市信義區信義路五段7號"

# 7. 提交訂單（用 Enter 觸發 React onSubmit，勿用 click @submitBtn）
agent-browser press Enter

# 8. 截圖確認成功頁
agent-browser screenshot > reports/success.png
```

> **注意**：`@eN` ref 每次 snapshot 後可能改變，執行前務必重新 `agent-browser snapshot` 確認。

---

## 驗證點

| 步驟 | 預期結果 |
|------|----------|
| 加入購物車後 | NavBar 徽章數字遞增 |
| 進入 /cart | 商品明細顯示正確 |
| 數量調整 | 小計與總計同步更新 |
| 結帳成功 | 導向 `/checkout/success`，顯示訂單明細 |
| 結帳後購物車 | NavBar 徽章消失，購物車清空 |

---

## 本專案 API 格式備忘

結帳 API 需要 `recipient` 包裝層：

```json
POST /api/cart/checkout
{
  "recipient": {
    "name": "王小明",
    "phone": "0912345678",
    "email": "test@example.com",
    "address": "台北市信義區信義路五段7號"
  }
}
```

詳細 SOP 與截圖參考：[reports/checkout-sop.md](../../../reports/checkout-sop.md)
