# API 文件 — Shopping Cart 購物車系統

> 依據 [prd.md](prd.md)、[spec.md](spec.md) 撰寫，RESTful 風格。
> 後端：Java 17 + Spring Boot 3.x，預設埠 **8083**。

---

## 1. 通用約定

### 1.1 基底資訊

| 項目 | 值 |
|------|-----|
| Base URL（本機） | `http://localhost:8083` |
| API 前綴 | `/api` |
| 內容型別 | `application/json; charset=utf-8` |
| 金額型別 | 數字（伺服器以 `BigDecimal` 計算後序列化） |
| 時間格式 | ISO-8601（UTC），如 `2026-06-03T08:00:00Z` |

### 1.2 訪客識別（Session）

- 所有購物車相關 API 以 cookie `SESSION_ID` 識別訪客。
- 首次呼叫若無此 cookie，伺服器自動產生並以 `Set-Cookie: SESSION_ID=...; HttpOnly; SameSite=Lax` 回傳。
- 前端後續請求需攜帶該 cookie（`fetch` 加 `credentials: 'include'`）。

### 1.3 金額權威原則

- 所有 `subtotal`、`total` 一律由伺服器計算回傳。
- **API 不接受客戶端傳入金額欄位**；若傳入將被忽略。

### 1.4 通用 HTTP 狀態碼

| 狀態碼 | 意義 |
|--------|------|
| 200 OK | 查詢 / 更新成功 |
| 201 Created | 建立成功 |
| 204 No Content | 刪除成功，無回應內容 |
| 400 Bad Request | 參數驗證失敗 / 購物車為空 / 庫存不足 |
| 404 Not Found | 資源不存在 |
| 500 Internal Server Error | 伺服器未預期錯誤 |

### 1.5 錯誤回應格式

統一由 `GlobalExceptionHandler`（`@RestControllerAdvice`）轉換：

```json
{
  "timestamp": "2026-06-03T08:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "庫存不足",
  "path": "/api/cart/checkout"
}
```

---

## 2. API 一覽

| # | Method | Path | 說明 |
|---|--------|------|------|
| 1 | GET | `/api/products` | 取得所有商品列表 |
| 2 | GET | `/api/products/{id}` | 取得單一商品 |
| 3 | GET | `/api/cart` | 取得當前購物車（含 total） |
| 4 | POST | `/api/cart/items` | 加入商品（自動合併數量） |
| 5 | PATCH | `/api/cart/items/{itemId}` | 更新數量（0 則刪除） |
| 6 | DELETE | `/api/cart/items/{itemId}` | 直接移除明細 |
| 7 | POST | `/api/cart/checkout` | 填入收件資料並結帳 |

---

## 3. 商品 Product

### 3.1 取得商品列表

```
GET /api/products
```

**回應 200**

```json
[
  {
    "id": "3f1a7c2e-1b2c-4d5e-8a9b-0c1d2e3f4a5b",
    "name": "商品 A",
    "description": "經典款式",
    "price": 299,
    "stock": 12,
    "imageUrl": "https://example.com/a.jpg",
    "createdAt": "2026-06-01T10:00:00Z"
  }
]
```

### 3.2 取得單一商品

```
GET /api/products/{id}
```

| 參數 | 位置 | 型別 | 說明 |
|------|------|------|------|
| `id` | path | UUID | 商品 ID |

**回應 200**

```json
{
  "id": "3f1a7c2e-1b2c-4d5e-8a9b-0c1d2e3f4a5b",
  "name": "商品 A",
  "description": "經典款式",
  "price": 299,
  "stock": 12,
  "imageUrl": "https://example.com/a.jpg",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

**回應 404** — 商品不存在。

---

## 4. 購物車 Cart

### 4.1 取得當前購物車

```
GET /api/cart
```

**回應 200**

```json
{
  "cartId": "9a8b7c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d",
  "items": [
    {
      "itemId": "11111111-1111-1111-1111-111111111111",
      "productId": "3f1a7c2e-1b2c-4d5e-8a9b-0c1d2e3f4a5b",
      "name": "商品 A",
      "unitPrice": 299,
      "quantity": 2,
      "subtotal": 598
    }
  ],
  "totalQuantity": 2,
  "total": 598
}
```

> `totalQuantity` = 所有 `quantity` 加總，供前端購物車徽章顯示（見 [spec.md §11.2](spec.md)）。
> 購物車為空時 `items` 為 `[]`、`total` 為 `0`。

---

### 4.2 加入商品（自動合併）

```
POST /api/cart/items
```

**請求 Body**

```json
{
  "productId": "3f1a7c2e-1b2c-4d5e-8a9b-0c1d2e3f4a5b",
  "quantity": 1
}
```

| 欄位 | 型別 | 必填 | 規則 |
|------|------|------|------|
| `productId` | UUID | ✔ | 必須存在 |
| `quantity` | int | ✔ | ≥ 1（預設 1） |

**行為**

- 若購物車已有相同 `productId` → **數量合併**（不新增明細列）。
- 新明細的 `unitPrice` 快照當下商品 `price`。

**回應 201** — 回傳更新後的完整購物車（同 §4.1 結構）。

```json
{
  "cartId": "9a8b7c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d",
  "items": [
    {
      "itemId": "11111111-1111-1111-1111-111111111111",
      "productId": "3f1a7c2e-1b2c-4d5e-8a9b-0c1d2e3f4a5b",
      "name": "商品 A",
      "unitPrice": 299,
      "quantity": 3,
      "subtotal": 897
    }
  ],
  "totalQuantity": 3,
  "total": 897
}
```

**回應 400** — `quantity < 1` 或缺欄位。
**回應 404** — `productId` 不存在。

---

### 4.3 更新明細數量（0 則刪除）

```
PATCH /api/cart/items/{itemId}
```

| 參數 | 位置 | 型別 | 說明 |
|------|------|------|------|
| `itemId` | path | UUID | 購物車明細 ID |

**請求 Body**

```json
{ "quantity": 0 }
```

| 欄位 | 型別 | 必填 | 規則 |
|------|------|------|------|
| `quantity` | int | ✔ | ≥ 0；為 0 時自動移除該明細 |

**行為**

- `quantity == 0` → 刪除該 `CartItem`。
- `quantity > 0` → 更新為該數量。
- `quantity < 0` → 400（前端亦會阻擋）。

**回應 200** — 回傳更新後的完整購物車（同 §4.1 結構）。
**回應 400** — `quantity < 0`。
**回應 404** — `itemId` 不存在。

---

### 4.4 移除明細

```
DELETE /api/cart/items/{itemId}
```

| 參數 | 位置 | 型別 | 說明 |
|------|------|------|------|
| `itemId` | path | UUID | 購物車明細 ID |

**回應 200** — 回傳更新後的完整購物車（同 §4.1 結構）。
**回應 404** — `itemId` 不存在。

---

## 5. 結帳 Checkout

### 5.1 結帳

```
POST /api/cart/checkout
```

**請求 Body**

```json
{
  "recipient": {
    "name": "王小明",
    "phone": "0912345678",
    "email": "user@example.com",
    "address": "台北市信義區信義路五段7號"
  }
}
```

| 欄位 | 型別 | 必填 | 規則（Bean Validation） |
|------|------|------|------|
| `recipient.name` | String | ✔ | 非空 |
| `recipient.phone` | String | ✔ | 符合電話格式 |
| `recipient.email` | String | ✔ | 符合 Email 格式 |
| `recipient.address` | String | ✔ | 非空 |

**行為（`@Transactional`）**

1. 驗證收件資料。
2. 驗證購物車非空。
3. 逐項以 `SELECT ... FOR UPDATE` 鎖列檢查庫存（`stock ≥ quantity`）。
4. 扣減各商品 `stock`。
5. 記錄收件資料。
6. 設定 `Cart.checkedOutAt = now()`（購物車清空）。

**回應 200** — 訂單摘要

```json
{
  "cartId": "9a8b7c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d",
  "checkedOutAt": "2026-06-03T08:05:00Z",
  "recipient": {
    "name": "王小明",
    "phone": "0912345678",
    "email": "user@example.com",
    "address": "台北市信義區信義路五段7號"
  },
  "items": [
    {
      "productId": "3f1a7c2e-1b2c-4d5e-8a9b-0c1d2e3f4a5b",
      "name": "商品 A",
      "unitPrice": 299,
      "quantity": 3,
      "subtotal": 897
    }
  ],
  "total": 897
}
```

**回應 400**

| message | 情境 |
|---------|------|
| `購物車為空` | 結帳時購物車無明細 |
| `庫存不足` | 任一商品 `stock < quantity`，整筆 rollback |
| 欄位驗證訊息 | 收件資料缺漏或格式錯誤 |

---

## 6. 狀態碼對照（依端點）

| 端點 | 成功 | 可能錯誤 |
|------|------|----------|
| GET /api/products | 200 | 500 |
| GET /api/products/{id} | 200 | 404 |
| GET /api/cart | 200 | 500 |
| POST /api/cart/items | 201 | 400, 404 |
| PATCH /api/cart/items/{itemId} | 200 | 400, 404 |
| DELETE /api/cart/items/{itemId} | 200 | 404 |
| POST /api/cart/checkout | 200 | 400 |
| POST /api/coupons/validate | 200 | 400, 404 |
| GET /api/coupons/available | 200 | 500 |
| GET /api/admin/coupons | 200 | 500 |
| POST /api/admin/coupons | 201 | 400 |
| PUT /api/admin/coupons/{id} | 200 | 400, 404 |
| DELETE /api/admin/coupons/{id} | 204 | 404 |

---

## 7. 對應流程

加入 / 更新 / 結帳的完整流程與序列圖請見：

- [spec.md §3 關鍵流程](spec.md)
- [spec.md §8 序列圖](spec.md)
- [spec.md §13.3 優惠券關鍵流程](spec.md)
- [spec.md §13.7 優惠券序列圖](spec.md)

---

## 8. 優惠券 Coupon

> 優惠券折扣金額一律由伺服器計算，前端不得自行計算折扣。
> 驗證 API 僅試算不消耗；正式消耗於結帳 `POST /api/cart/checkout` 時執行。

### 8.1 結帳 API 更新（支援優惠券）

```
POST /api/cart/checkout
```

**請求 Body**（新增 `couponCode` 選填欄位）

```json
{
  "recipient": {
    "name": "王小明",
    "phone": "0912345678",
    "email": "user@example.com",
    "address": "台北市信義區信義路五段7號"
  },
  "couponCode": "WELCOME10"
}
```

| 欄位 | 型別 | 必填 | 規則 |
|------|------|------|------|
| `couponCode` | String | 選填 | 優惠券代碼（若傳入則驗證並套用折扣） |

**回應 200**（新增折扣相關欄位）

```json
{
  "cartId": "9a8b7c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d",
  "checkedOutAt": "2026-06-14T08:05:00Z",
  "recipient": {
    "name": "王小明",
    "phone": "0912345678",
    "email": "user@example.com",
    "address": "台北市信義區信義路五段7號"
  },
  "items": [
    {
      "productId": "3f1a7c2e-1b2c-4d5e-8a9b-0c1d2e3f4a5b",
      "name": "Arc Floor Lamp",
      "unitPrice": 12800,
      "quantity": 1,
      "subtotal": 12800
    }
  ],
  "total": 12800,
  "discountAmount": 1280,
  "finalTotal": 11520,
  "couponCode": "WELCOME10"
}
```

| 欄位 | 說明 |
|------|------|
| `total` | 折扣前合計（原始金額） |
| `discountAmount` | 折扣金額（伺服器計算，無優惠券時為 0） |
| `finalTotal` | 折扣後實付金額（= total - discountAmount） |
| `couponCode` | 套用的優惠券代碼（無優惠券時為 null） |

---

### 8.2 優惠券驗證試算

```
POST /api/coupons/validate
```

> 僅試算折扣，**不消耗優惠券使用次數**。

**請求 Body**

```json
{
  "code": "WELCOME10",
  "orderAmount": 12800
}
```

| 欄位 | 型別 | 必填 | 規則 |
|------|------|------|------|
| `code` | String | ✔ | 優惠券代碼（不分大小寫，系統統一轉大寫處理） |
| `orderAmount` | Number | ✔ | 訂單原始金額（> 0） |

**回應 200**

```json
{
  "code": "WELCOME10",
  "name": "新會員優惠",
  "description": "新會員首次購物享 10% 折扣",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "discountAmount": 1280,
  "originalAmount": 12800,
  "finalAmount": 11520
}
```

| 欄位 | 說明 |
|------|------|
| `code` | 優惠券代碼 |
| `name` | 優惠券名稱 |
| `description` | 優惠券說明 |
| `discountType` | 折扣類型：`PERCENTAGE`（百分比）/ `FIXED`（固定金額） |
| `discountValue` | 折扣值（百分比：0~100；固定：TWD 金額） |
| `discountAmount` | 折扣金額（伺服器計算） |
| `originalAmount` | 原始訂單金額（等同請求 `orderAmount`） |
| `finalAmount` | 折扣後金額（= originalAmount - discountAmount） |

**回應 400**

| message | 情境 |
|---------|------|
| `優惠券已停用` | `isActive = false` |
| `優惠券尚未開始` | 當前時間早於 `startDate` |
| `優惠券已過期` | 當前時間晚於 `endDate` |
| `未達最低消費門檻 $N` | `orderAmount < minOrderAmount` |
| `優惠券使用次數已達上限` | `usageCount >= maxUsageCount`（maxUsageCount 不為 null） |

**回應 404** — 優惠券代碼不存在。

---

### 8.3 查詢可用優惠券列表

```
GET /api/coupons/available
```

> 回傳目前啟用中且在有效期限內的所有優惠券（供前端選擇清單使用）。
> 不含已超過使用次數上限的優惠券。

**回應 200**

```json
[
  {
    "code": "WELCOME10",
    "name": "新會員優惠",
    "description": "新會員首次購物享 10% 折扣",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "minOrderAmount": 0,
    "endDate": "2026-12-31T23:59:59Z"
  },
  {
    "code": "SAVE500",
    "name": "滿千折五百",
    "description": "訂單金額滿 $1,000 折 $500",
    "discountType": "FIXED",
    "discountValue": 500,
    "minOrderAmount": 1000,
    "endDate": "2026-09-30T23:59:59Z"
  }
]
```

---

## 9. 優惠券後台管理（Admin）

> 後台管理 API，供管理員新增、修改、停用、刪除優惠券。

### 9.1 取得所有優惠券（含停用）

```
GET /api/admin/coupons
```

**回應 200**

```json
[
  {
    "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
    "code": "WELCOME10",
    "name": "新會員優惠",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "minOrderAmount": 0,
    "maxUsageCount": 100,
    "usageCount": 5,
    "startDate": "2026-01-01T00:00:00Z",
    "endDate": "2026-12-31T23:59:59Z",
    "isActive": true,
    "description": "新會員首次購物享 10% 折扣",
    "createdAt": "2026-06-14T00:00:00Z"
  }
]
```

---

### 9.2 建立優惠券

```
POST /api/admin/coupons
```

**請求 Body**

```json
{
  "code": "SUMMER20",
  "name": "夏季促銷",
  "discountType": "PERCENTAGE",
  "discountValue": 20,
  "minOrderAmount": 500,
  "maxUsageCount": 50,
  "startDate": "2026-07-01T00:00:00Z",
  "endDate": "2026-08-31T23:59:59Z",
  "isActive": true,
  "description": "夏季購物享 20% 折扣，最低消費 $500"
}
```

| 欄位 | 型別 | 必填 | 規則 |
|------|------|------|------|
| `code` | String | ✔ | 英數字，長度 1~50，不重複 |
| `name` | String | ✔ | 非空，最多 255 字元 |
| `discountType` | String | ✔ | `PERCENTAGE` 或 `FIXED` |
| `discountValue` | Number | ✔ | PERCENTAGE：0~100；FIXED：> 0 |
| `minOrderAmount` | Number | 選填 | ≥ 0，預設 0 |
| `maxUsageCount` | Integer | 選填 | > 0（null 表示無限制） |
| `startDate` | ISO-8601 | ✔ | 有效開始時間 |
| `endDate` | ISO-8601 | ✔ | 有效截止時間（需晚於 startDate） |
| `isActive` | Boolean | 選填 | 預設 true |
| `description` | String | 選填 | 優惠券說明（顯示給使用者） |

**回應 201** — 回傳建立後的完整 Coupon 物件（同 §9.1 結構）。

**回應 400** — 欄位驗證失敗 / 代碼已存在。

---

### 9.3 更新優惠券

```
PUT /api/admin/coupons/{id}
```

| 參數 | 位置 | 型別 | 說明 |
|------|------|------|------|
| `id` | path | UUID | 優惠券 ID |

**請求 Body** — 同 §9.2，所有欄位可更新（code 除外，建立後不可修改）。

**回應 200** — 回傳更新後的完整 Coupon 物件。
**回應 404** — 優惠券不存在。
**回應 400** — 欄位驗證失敗。

---

### 9.4 刪除優惠券

```
DELETE /api/admin/coupons/{id}
```

| 參數 | 位置 | 型別 | 說明 |
|------|------|------|------|
| `id` | path | UUID | 優惠券 ID |

**回應 204** — 刪除成功（無回應 Body）。
**回應 404** — 優惠券不存在。
