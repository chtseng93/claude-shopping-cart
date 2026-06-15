# 規格文件 — Shopping Cart 購物車系統

> 本文件依據 [prd.md](prd.md) 撰寫，作為開發前與開發者確認之規格依據。
> 流程圖一律使用 mermaid 製作。

---

## 1. 架構與選型

### 1.1 整體架構

採前後端分離的三層式架構（Client → API Server → Database）。

| 層級 | 技術 | 選型理由 |
|------|------|----------|
| 前端 | React + Vite | Vite 提供快速 HMR 與輕量打包；React 元件化利於購物車狀態管理 |
| 後端 | Java 17 + Spring Boot 3.x | 企業級成熟生態；Spring Data JPA 加速資料層開發；`@Transactional` 保證結帳一致性 |
| 資料庫 | PostgreSQL 18 | 交易（transaction）保證結帳一致性；NUMERIC 型別精確處理金額 |
| 建置 | Maven | pom.xml 設定直觀、生態普及 |
| 容器 | Docker（`docker compose`） | 一致的本機/部署環境，DB 一鍵啟動 |

### 1.2 關鍵設計原則

- **金額權威在伺服器**：購物車合計（total）、小計（subtotal）一律由後端計算回傳，API 不接受客戶端傳入金額。
- **單價快照**：商品加入購物車時，將當下 `price` 寫入 `CartItem.unitPrice`，避免商品調價影響既有購物車。
- **訪客 Session**：以 `sessionId`（cookie）識別訪客購物車，無需會員登入。
- **庫存驗證時機**：僅於「結帳」時檢查庫存，加入購物車時不檢查。

### 1.3 技術選型一覽

```
後端：Java 17 / Spring Boot 3.x / Spring Web / Spring Data JPA (Hibernate) / Bean Validation
前端：React / Vite / fetch 或 axios
資料庫：PostgreSQL 18
建置：Maven
容器：Docker + docker compose
JDK：C:\Program Files\Java\openjdk-17.0.12
```

---

## 2. 資料模型

> JPA Entity 欄位以 camelCase 命名，資料表欄位以 snake_case（透過命名策略對應）。

### 2.1 Product（商品）

| Entity 欄位 | 資料表欄位 | Java 型別 | DB 型別 / 約束 | 說明 |
|------|------|------|------|------|
| `id` | `id` | UUID | UUID PK | 主鍵 |
| `name` | `name` | String | VARCHAR(255) NOT NULL | 商品名稱 |
| `description` | `description` | String | TEXT | 商品描述 |
| `price` | `price` | BigDecimal | NUMERIC(10,2) NOT NULL，CHECK ≥ 0 | 單價（TWD） |
| `stock` | `stock` | Integer | INTEGER NOT NULL，CHECK ≥ 0 | 庫存數量 |
| `imageUrl` | `image_url` | String | TEXT | 商品圖片 URL |
| `createdAt` | `created_at` | Instant | TIMESTAMPTZ 預設 now() | 建立時間 |

### 2.2 Cart（購物車）

| Entity 欄位 | 資料表欄位 | Java 型別 | DB 型別 / 約束 | 說明 |
|------|------|------|------|------|
| `id` | `id` | UUID | UUID PK | 主鍵 |
| `sessionId` | `session_id` | String | VARCHAR(255) NOT NULL，INDEX | 訪客 session 識別 |
| `createdAt` | `created_at` | Instant | TIMESTAMPTZ 預設 now() | 建立時間 |
| `checkedOutAt` | `checked_out_at` | Instant | TIMESTAMPTZ NULL | 結帳時間（NULL 表未結帳） |
| `items` | — | List&lt;CartItem&gt; | `@OneToMany(mappedBy="cart")` | 明細關聯 |

### 2.3 CartItem（購物車明細）

| Entity 欄位 | 資料表欄位 | Java 型別 | DB 型別 / 約束 | 說明 |
|------|------|------|------|------|
| `id` | `id` | UUID | UUID PK | 主鍵 |
| `cart` | `cart_id` | Cart | `@ManyToOne` FK NOT NULL | 所屬購物車 |
| `product` | `product_id` | Product | `@ManyToOne` FK NOT NULL | 對應商品 |
| `quantity` | `quantity` | Integer | INTEGER NOT NULL，CHECK ≥ 1 | 數量 |
| `unitPrice` | `unit_price` | BigDecimal | NUMERIC(10,2) NOT NULL | 加入當下快照單價 |

> 唯一約束：`@UniqueConstraint(columnNames = {"cart_id", "product_id"})` — 確保同一購物車中同商品僅一列，達成「自動合併數量」。

詳細關聯見 [§9 ER 圖](#9-er-圖)。

---

## 3. 關鍵流程

### 3.1 加入購物車（自動合併）

```mermaid
flowchart TD
    A[使用者點擊加入購物車] --> B{購物車是否存在}
    B -- 否 --> C[建立 Cart]
    B -- 是 --> D[取得 Cart]
    C --> E{該 product 是否已在購物車}
    D --> E
    E -- 否 --> F[新增 CartItem<br/>快照 unitPrice]
    E -- 是 --> G[既有 CartItem.quantity += 數量]
    F --> H[伺服器重新計算 total]
    G --> H
    H --> I[回傳購物車與 total]
```

### 3.2 更新數量（0 則移除）

```mermaid
flowchart TD
    A[使用者修改數量] --> B{數量 < 0?}
    B -- 是 --> C[前端阻擋，不送請求]
    B -- 否 --> D[PATCH /api/cart/items/:itemId]
    D --> E{數量 == 0?}
    E -- 是 --> F[刪除該 CartItem]
    E -- 否 --> G[更新 quantity]
    F --> H[重新計算 total]
    G --> H
    H --> I[回傳購物車與 total]
```

### 3.3 結帳流程

```mermaid
flowchart TD
    A[使用者填寫收件資料送出] --> B[POST /api/cart/checkout]
    B --> C{購物車是否為空}
    C -- 是 --> D[回傳 400 購物車為空]
    C -- 否 --> E[進入 @Transactional 方法]
    E --> F{逐項檢查庫存<br/>stock >= quantity}
    F -- 不足 --> G[拋例外觸發 Rollback，回傳 400 庫存不足]
    F -- 充足 --> H[扣減各商品 stock]
    H --> I[記錄收件資料]
    I --> J[設定 checkedOutAt = now]
    J --> K[方法正常返回，交易 Commit]
    K --> L[回傳訂單摘要含 total]
```

---

## 4. 虛擬碼

> 以 Spring Boot Service 層表示，註解使用中文。

### 4.1 加入購物車

```java
// 加入商品至購物車，若已存在則合併數量
@Transactional
CartResponse addToCart(String sessionId, UUID productId, int quantity) {
    Cart cart = cartRepo.findActiveBySession(sessionId)
                        .orElseGet(() -> cartRepo.save(new Cart(sessionId)));
    Product product = productRepo.findById(productId)
                        .orElseThrow(() -> new NotFoundException());

    cartItemRepo.findByCartAndProduct(cart.getId(), productId)
        .ifPresentOrElse(
            item -> item.setQuantity(item.getQuantity() + quantity),   // 自動合併
            ()   -> cartItemRepo.save(new CartItem(cart, product, quantity, product.getPrice())) // 快照單價
        );

    return buildCartResponse(cart.getId());   // total 由伺服器計算
}
```

### 4.2 更新數量

```java
// 更新明細數量，數量為 0 則移除
@Transactional
CartResponse updateItemQuantity(UUID itemId, int quantity) {
    if (quantity < 0) throw new BadRequestException();   // 防呆（前端亦阻擋）
    CartItem item = cartItemRepo.findById(itemId)
                        .orElseThrow(() -> new NotFoundException());

    if (quantity == 0) {
        cartItemRepo.delete(item);            // 數量 0 自動移除
    } else {
        item.setQuantity(quantity);
    }
    return buildCartResponse(item.getCart().getId());
}
```

### 4.3 計算合計（伺服器權威）

```java
// 建立購物車回應，subtotal 與 total 一律由伺服器計算
CartResponse buildCartResponse(UUID cartId) {
    List<CartItem> items = cartItemRepo.findByCartId(cartId);
    List<ItemDto> dtos = items.stream().map(it -> {
        BigDecimal subtotal = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
        return new ItemDto(it, subtotal);
    }).toList();
    BigDecimal total = dtos.stream()
                           .map(ItemDto::subtotal)
                           .reduce(BigDecimal.ZERO, BigDecimal::add);   // 不接受客戶端傳入
    return new CartResponse(cartId, dtos, total);
}
```

### 4.4 結帳

```java
// 結帳：驗證、鎖庫存、扣減、記錄收件資料、清空（標記已結帳）
@Transactional
OrderSummary checkout(String sessionId, RecipientDto recipient) {
    validate(recipient);                       // Bean Validation：name/phone/email/address
    Cart cart = cartRepo.findActiveBySession(sessionId)
                        .orElseThrow(() -> new BadRequestException());
    List<CartItem> items = cartItemRepo.findByCartId(cart.getId());
    if (items.isEmpty()) throw new BadRequestException("購物車為空");

    for (CartItem it : items) {
        Product p = productRepo.lockById(it.getProduct().getId());   // SELECT ... FOR UPDATE
        if (p.getStock() < it.getQuantity()) throw new BadRequestException("庫存不足"); // 觸發 rollback
        p.setStock(p.getStock() - it.getQuantity());                 // 扣減庫存
    }
    saveRecipient(cart.getId(), recipient);
    cart.setCheckedOutAt(Instant.now());        // 標記已結帳 -> 購物車清空
    return new OrderSummary(cart, items, buildCartResponse(cart.getId()).total());
}
```

---

## 5. 系統脈絡圖

```mermaid
graph LR
    User([訪客 / 使用者])
    subgraph System[Shopping Cart 系統]
        FE[React 前端]
        BE[Spring Boot API]
        DB[(PostgreSQL)]
    end
    User -->|瀏覽商品 / 操作購物車 / 結帳| FE
    FE -->|REST API / JSON| BE
    BE -->|JPA / SQL| DB
```

---

## 6. 容器/部署概觀

```mermaid
graph TD
    subgraph Host[本機 / 部署主機]
        subgraph Compose[docker compose]
            FEC[frontend<br/>React + Vite<br/>:5173]
            BEC[backend<br/>Java 17 + Spring Boot<br/>:8083]
            DBC[(db<br/>PostgreSQL 18<br/>:5432)]
            VOL[[pg_data volume]]
        end
    end
    Browser([瀏覽器]) --> FEC
    FEC -->|VITE_API_BASE_URL| BEC
    BEC -->|SPRING_DATASOURCE_URL| DBC
    DBC --- VOL
```

部署要點：

- 提供 `docker-compose.yml`（以 `docker compose up` 啟動）。
- 後端讀取環境變數 `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`；前端讀取 `VITE_API_BASE_URL`。
- 後端以多階段 Dockerfile（Maven build → JRE 17 runtime）打包。
- DB 資料以具名 volume（`pg_data`）持久化。

---

## 7. 模組關係圖（Backend / Frontend）

### 7.1 Backend（Spring Boot 分層）

```mermaid
graph TD
    C[Controller<br/>@RestController] --> S[Service<br/>@Service]
    S --> Repo[Repository<br/>Spring Data JPA]
    Repo --> DBpg[(PostgreSQL)]
    S --> Calc[CartCalculator<br/>合計計算]
    C --> EH[GlobalExceptionHandler<br/>@RestControllerAdvice]
    C --> Val[Bean Validation<br/>@Valid]
    F[SessionFilter<br/>session 識別] --> C
```

| 模組 | 職責 |
|------|------|
| Controller | 接收 HTTP 請求、回傳回應，不含商業邏輯 |
| Service | 商業邏輯（加入合併、移除、結帳、合計），標註 `@Transactional` |
| Repository | Spring Data JPA 資料存取介面 |
| CartCalculator | 計算 subtotal / total（伺服器權威） |
| GlobalExceptionHandler | 統一例外轉 HTTP 狀態碼 |
| SessionFilter | 由 cookie 解析 / 建立 sessionId |

### 7.2 Frontend

```mermaid
graph TD
    App[App] --> Pages[pages]
    Pages --> P1[ProductListPage]
    Pages --> P2[CartPage]
    Pages --> P3[CheckoutPage]
    P1 --> Comp[components]
    P2 --> Comp
    P3 --> Comp
    Comp --> API[api client]
    Pages --> Store[cart context / state]
    Store --> API
    API -->|fetch| BE[後端 REST API]
```

---

## 8. 序列圖

### 8.1 加入購物車

```mermaid
sequenceDiagram
    participant U as 使用者
    participant FE as React 前端
    participant BE as Spring Boot API
    participant DB as PostgreSQL

    U->>FE: 點擊「加入購物車」
    FE->>BE: POST /api/cart/items {productId, quantity}
    BE->>DB: 查詢 / 建立 Cart
    BE->>DB: 查詢同商品 CartItem
    alt 已存在
        BE->>DB: UPDATE quantity += n
    else 不存在
        BE->>DB: INSERT CartItem (快照 unitPrice)
    end
    BE->>DB: 查詢全部 items
    BE->>BE: 計算 subtotal / total
    BE-->>FE: 200 {items, total}
    FE-->>U: 更新購物車顯示
```

### 8.2 結帳

```mermaid
sequenceDiagram
    participant U as 使用者
    participant FE as React 前端
    participant BE as Spring Boot API
    participant DB as PostgreSQL

    U->>FE: 填寫收件資料並送出
    FE->>BE: POST /api/cart/checkout {recipient}
    BE->>BE: 驗證收件資料 / 購物車非空
    BE->>DB: 開始交易 @Transactional
    BE->>DB: SELECT ... FOR UPDATE 各商品
    alt 庫存不足
        BE->>BE: 拋出例外
        BE->>DB: ROLLBACK
        BE-->>FE: 400 庫存不足
    else 庫存充足
        BE->>DB: UPDATE stock 扣減
        BE->>DB: 記錄收件資料
        BE->>DB: UPDATE checked_out_at = now()
        BE->>DB: COMMIT
        BE-->>FE: 200 訂單摘要 {total}
        FE-->>U: 顯示結帳成功，清空購物車
    end
```

---

## 9. ER 圖

```mermaid
erDiagram
    PRODUCT ||--o{ CART_ITEM : "被加入"
    CART ||--o{ CART_ITEM : "包含"

    PRODUCT {
        uuid id PK
        varchar name
        text description
        numeric price
        int stock
        text image_url
        timestamptz created_at
    }
    CART {
        uuid id PK
        varchar session_id
        timestamptz created_at
        timestamptz checked_out_at
    }
    CART_ITEM {
        uuid id PK
        uuid cart_id FK
        uuid product_id FK
        int quantity
        numeric unit_price
    }
```

---

## 10. 類別圖（Class Diagram）

呈現後端 Spring Boot 主要類別（Entity / DTO / Controller / Service / Repository）的屬性、方法與關聯。

```mermaid
classDiagram
    direction LR

    class Product {
        +UUID id
        +String name
        +String description
        +BigDecimal price
        +Integer stock
        +String imageUrl
        +Instant createdAt
    }

    class Cart {
        +UUID id
        +String sessionId
        +Instant createdAt
        +Instant checkedOutAt
        +List~CartItem~ items
    }

    class CartItem {
        +UUID id
        +Cart cart
        +Product product
        +Integer quantity
        +BigDecimal unitPrice
    }

    class CartResponse {
        +UUID cartId
        +List~ItemDto~ items
        +int totalQuantity
        +BigDecimal total
    }

    class ItemDto {
        +UUID itemId
        +UUID productId
        +String name
        +BigDecimal unitPrice
        +int quantity
        +BigDecimal subtotal
    }

    class RecipientDto {
        +String name
        +String phone
        +String email
        +String address
    }

    class OrderSummary {
        +UUID cartId
        +Instant checkedOutAt
        +RecipientDto recipient
        +List~ItemDto~ items
        +BigDecimal total
    }

    class ProductController {
        +list() List~Product~
        +get(UUID id) Product
    }

    class CartController {
        +getCart() CartResponse
        +addItem(AddItemReq) CartResponse
        +updateItem(UUID, UpdateReq) CartResponse
        +deleteItem(UUID) CartResponse
        +checkout(RecipientDto) OrderSummary
    }

    class CartService {
        +addToCart(sessionId, productId, qty) CartResponse
        +updateItemQuantity(itemId, qty) CartResponse
        +removeItem(itemId) CartResponse
        +getCart(sessionId) CartResponse
        +checkout(sessionId, RecipientDto) OrderSummary
        -buildCartResponse(cartId) CartResponse
    }

    class ProductRepository {
        <<interface>>
        +findAll() List~Product~
        +findById(UUID) Optional~Product~
        +lockById(UUID) Product
    }

    class CartRepository {
        <<interface>>
        +findActiveBySession(String) Optional~Cart~
        +save(Cart) Cart
    }

    class CartItemRepository {
        <<interface>>
        +findByCartId(UUID) List~CartItem~
        +findByCartAndProduct(UUID, UUID) Optional~CartItem~
        +delete(CartItem) void
    }

    Cart "1" o-- "*" CartItem : 包含
    Product "1" <-- "*" CartItem : 對應
    CartResponse "1" *-- "*" ItemDto
    OrderSummary "1" *-- "*" ItemDto
    OrderSummary --> RecipientDto

    ProductController ..> Product
    CartController ..> CartService
    CartController ..> CartResponse
    CartController ..> OrderSummary
    CartService ..> CartRepository
    CartService ..> CartItemRepository
    CartService ..> ProductRepository
    CartService ..> CartResponse
    ProductRepository ..> Product
    CartRepository ..> Cart
    CartItemRepository ..> CartItem
```

> 說明：`CartResponse` / `ItemDto` / `OrderSummary` / `RecipientDto` 為傳輸用 DTO（金額由伺服器計算後填入）；Repository 為 Spring Data JPA 介面。

---

## 11. 畫面設計與互動

### 11.1 頁面清單

| 頁面 | 路由 | 說明 |
|------|------|------|
| 商品列表頁 | `/` | 卡片列表呈現所有商品，含「加入購物車」按鈕 |
| 購物車頁 | `/cart` | 明細列表、數量調整、伺服器合計 |
| 結帳頁 | `/checkout` | 收件資料表單 |
| 結帳成功頁 | `/checkout/success` | 訂單摘要，購物車已清空 |

所有頁面頂部共用 **導覽列（NavBar）**，右側固定顯示購物車圖示與數量徽章。

### 11.2 共用導覽列（含購物車徽章）

```
┌──────────────────────────────────────────────┐
│  🛒 Shopping Cart            [🛒 商品]  ( 3 )  │  ← 徽章顯示購物車件數
└──────────────────────────────────────────────┘
```

- 徽章數字 = 購物車內所有 `CartItem.quantity` 加總（由 `GET /api/cart` 回傳資料計算）。
- 件數為 0 時隱藏徽章。

### 11.3 商品列表頁（Wireframe）

```
┌──────────────────────────────────────────────┐
│  NavBar                            [🛒] ( 2 )  │
├──────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ [圖片]   │  │ [圖片]   │  │ [圖片]   │    │
│  │ 商品 A   │  │ 商品 B   │  │ 商品 C   │    │
│  │ $299     │  │ $499     │  │ $159     │    │
│  │ 庫存 12  │  │ 庫存 5   │  │ 庫存 0   │    │
│  │[加入購物車]│ │[加入購物車]│ │[ 已售完 ]│    │
│  └──────────┘  └──────────┘  └──────────┘    │
└──────────────────────────────────────────────┘
```

### 11.4 購物車頁（Wireframe）

```
┌──────────────────────────────────────────────┐
│  購物車                                        │
├──────────────────────────────────────────────┤
│  商品 A    $299   [ − ] 2 [ + ]   小計 $598   │
│  商品 B    $499   [ − ] 1 [ + ]   小計 $499   │
│  ──────────────────────────────────────────   │
│                          合計（伺服器）$1097   │
│                                  [ 前往結帳 ]   │
└──────────────────────────────────────────────┘
```

- 小計、合計皆顯示伺服器回傳值，前端不自行加總。
- 數量降到 0（連按 `−`）→ 該列淡出移除。

### 11.5 結帳頁與成功頁

- **結帳頁**：收件表單（姓名、電話、Email、地址），欄位即時驗證；送出鈕在表單有效時才可點擊。
- **成功頁**：顯示訂單摘要（品項、合計），導覽列徽章歸零（購物車已清空）。

### 11.6 視覺與互動規範

- 動畫時長建議 150–300ms，使用 CSS transition / transform，避免阻塞操作。
- 所有金額顯示一律來自伺服器回應，前端僅負責呈現。
- 互動需具無障礙考量：徽章數字變更時更新 `aria-label`（如「購物車 3 件」）。

---

## 12. 範圍邊界（對應 PRD）

**包含**：商品瀏覽、加入購物車（合併）、數量更新（0 移除）、伺服器合計、結帳清空、**優惠券驗證與折扣**。

**不包含**：會員登入、金流串接、訂單歷史查詢。

---

> 後續：API 細節請見 `api.md`（RESTful 風格）。開發前請就本規格與開發者確認。

---

## 13. 優惠券模組（Coupon Feature）

> 依據 [coupon.md](coupon.md) 需求規格撰寫。

### 13.1 架構說明

優惠券模組採用伺服器端折扣計算，前端僅負責送出優惠券代碼並顯示 API 回傳的折扣結果。
折扣金額不接受客戶端傳入（金額權威在伺服器，與購物車合計一致）。

| 元件 | 職責 |
|------|------|
| `Coupon` Entity | 儲存優惠券主檔（代碼、折扣類型、使用限制、有效期間） |
| `CouponUsage` Entity | 記錄每次優惠券使用（與訂單對應） |
| `CouponService` | 驗證、試算、消耗、返還邏輯 |
| `CouponController` | 提供前端驗證試算 API 與後台管理 API |

### 13.2 資料模型

#### 13.2.1 Coupon（優惠券主檔）

| Entity 欄位 | 資料表欄位 | Java 型別 | DB 型別 / 約束 | 說明 |
|------|------|------|------|------|
| `id` | `id` | UUID | UUID PK | 主鍵 |
| `code` | `code` | String | VARCHAR(50) NOT NULL UNIQUE | 優惠券代碼（唯一，大寫英數） |
| `name` | `name` | String | VARCHAR(255) NOT NULL | 優惠券名稱（顯示用） |
| `discountType` | `discount_type` | DiscountType | VARCHAR(20) NOT NULL | 折扣類型：PERCENTAGE（百分比）/ FIXED（固定金額） |
| `discountValue` | `discount_value` | BigDecimal | NUMERIC(10,2) NOT NULL CHECK > 0 | 折扣值（百分比：0~100；固定：TWD 金額） |
| `minOrderAmount` | `min_order_amount` | BigDecimal | NUMERIC(10,2) NOT NULL DEFAULT 0 | 最低訂單金額門檻 |
| `maxUsageCount` | `max_usage_count` | Integer | INTEGER NULL | 全局最大使用次數（NULL 表示無限制） |
| `usageCount` | `usage_count` | Integer | INTEGER NOT NULL DEFAULT 0 | 已使用次數 |
| `startDate` | `start_date` | Instant | TIMESTAMPTZ NOT NULL | 有效開始日期 |
| `endDate` | `end_date` | Instant | TIMESTAMPTZ NOT NULL | 有效截止日期 |
| `isActive` | `is_active` | Boolean | BOOLEAN NOT NULL DEFAULT true | 是否啟用 |
| `description` | `description` | String | TEXT NULL | 優惠券說明（顯示給使用者） |
| `createdAt` | `created_at` | Instant | TIMESTAMPTZ DEFAULT now() | 建立時間 |

#### 13.2.2 CouponUsage（優惠券使用記錄）

| Entity 欄位 | 資料表欄位 | Java 型別 | DB 型別 / 約束 | 說明 |
|------|------|------|------|------|
| `id` | `id` | UUID | UUID PK | 主鍵 |
| `coupon` | `coupon_id` | Coupon | UUID FK NOT NULL | 對應優惠券 |
| `cartId` | `cart_id` | UUID | UUID NOT NULL | 對應已結帳購物車（訂單） |
| `sessionId` | `session_id` | String | VARCHAR(255) NOT NULL | 使用者 session 識別 |
| `discountAmount` | `discount_amount` | BigDecimal | NUMERIC(10,2) NOT NULL | 實際折扣金額（伺服器計算） |
| `usedAt` | `used_at` | Instant | TIMESTAMPTZ DEFAULT now() | 使用時間 |

### 13.3 關鍵流程

#### 13.3.1 優惠券驗證試算流程

```mermaid
flowchart TD
    A[使用者輸入優惠券代碼] --> B[POST /api/coupons/validate]
    B --> C{優惠券代碼是否存在}
    C -- 否 --> E1[回傳 404 優惠券不存在]
    C -- 是 --> D{isActive = true?}
    D -- 否 --> E2[回傳 400 優惠券已停用]
    D -- 是 --> F{是否在有效期限內}
    F -- 否 --> E3[回傳 400 優惠券已過期]
    F -- 是 --> G{訂單金額 >= minOrderAmount?}
    G -- 否 --> E4[回傳 400 未達最低消費門檻]
    G -- 是 --> H{使用次數未達上限?}
    H -- 否 --> E5[回傳 400 優惠券使用次數已達上限]
    H -- 是 --> I[計算折扣金額]
    I --> J[回傳 200 折扣試算結果]
```

#### 13.3.2 訂單成立後優惠券消耗流程

```mermaid
flowchart TD
    A[結帳 POST /api/cart/checkout] --> B{是否攜帶 couponCode?}
    B -- 否 --> C[正常結帳，無折扣]
    B -- 是 --> D[再次驗證優惠券]
    D --> E{驗證通過?}
    E -- 否 --> F[拋出例外，整筆 Rollback]
    E -- 是 --> G[計算折扣金額]
    G --> H[庫存扣減]
    H --> I[建立 CouponUsage 記錄]
    I --> J[coupon.usageCount += 1]
    J --> K[COMMIT]
    K --> L[回傳訂單摘要含折扣金額]
```

#### 13.3.3 付款失敗 / 訂單取消後優惠券返還流程

```mermaid
flowchart TD
    A[訂單取消] --> B{是否有關聯 CouponUsage?}
    B -- 否 --> C[無需處理]
    B -- 是 --> D[coupon.usageCount -= 1]
    D --> E[刪除 CouponUsage 記錄]
    E --> F[返還完成]
```

### 13.4 虛擬碼

```java
// 優惠券驗證試算（不消耗，僅計算折扣金額）
CouponValidateResponse validateCoupon(String code, BigDecimal orderAmount) {
    Coupon coupon = couponRepo.findByCode(code)
        .orElseThrow(() -> new NotFoundException("優惠券不存在"));

    // 逐項驗證規則
    if (!coupon.isActive()) throw new BadRequestException("優惠券已停用");
    Instant now = Instant.now();
    if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate()))
        throw new BadRequestException("優惠券已過期");
    if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0)
        throw new BadRequestException("未達最低消費門檻 $" + coupon.getMinOrderAmount());
    if (coupon.getMaxUsageCount() != null
        && coupon.getUsageCount() >= coupon.getMaxUsageCount())
        throw new BadRequestException("優惠券使用次數已達上限");

    // 由伺服器計算折扣金額
    BigDecimal discount = calculateDiscount(coupon, orderAmount);
    BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);
    return new CouponValidateResponse(coupon, discount, finalAmount);
}

// 折扣金額計算（伺服器權威）
BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
    return switch (coupon.getDiscountType()) {
        case PERCENTAGE ->
            orderAmount.multiply(coupon.getDiscountValue())
                       .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        case FIXED ->
            coupon.getDiscountValue().min(orderAmount);  // 折扣不超過訂單金額
    };
}

// 消耗優惠券（於結帳 @Transactional 內呼叫）
void consumeCoupon(Coupon coupon, UUID cartId, String sessionId, BigDecimal discountAmount) {
    CouponUsage usage = new CouponUsage(coupon, cartId, sessionId, discountAmount);
    couponUsageRepo.save(usage);
    coupon.setUsageCount(coupon.getUsageCount() + 1);  // 更新使用次數
}

// 返還優惠券（訂單取消時呼叫）
void releaseCoupon(UUID cartId) {
    couponUsageRepo.findByCartId(cartId).ifPresent(usage -> {
        Coupon coupon = usage.getCoupon();
        coupon.setUsageCount(Math.max(0, coupon.getUsageCount() - 1));
        couponUsageRepo.delete(usage);
    });
}
```

### 13.5 系統脈絡圖（含優惠券模組）

```mermaid
graph LR
    User([訪客 / 使用者])
    subgraph System[Shopping Cart 系統]
        FE[React 前端]
        subgraph BE[Spring Boot API]
            CartMod[Cart 模組]
            CouponMod[Coupon 模組]
        end
        DB[(PostgreSQL)]
    end
    User -->|瀏覽商品 / 操作購物車 / 輸入折扣碼 / 結帳| FE
    FE -->|REST API / JSON| CartMod
    FE -->|POST /api/coupons/validate| CouponMod
    CartMod -->|結帳時呼叫 consumeCoupon| CouponMod
    CartMod -->|JPA / SQL| DB
    CouponMod -->|JPA / SQL| DB
```

### 13.6 模組關係圖（含優惠券）

```mermaid
graph TD
    CC[CouponController] --> CS[CouponService]
    CS --> CR[CouponRepository]
    CS --> CUR[CouponUsageRepository]
    CR --> DBpg[(PostgreSQL)]
    CUR --> DBpg
    CartController --> CartService
    CartService --> CS
    CartService --> CartRepo[CartRepository]
    CartService --> ItemRepo[CartItemRepository]
    CartService --> ProductRepo[ProductRepository]
    CartRepo --> DBpg
    ItemRepo --> DBpg
    ProductRepo --> DBpg
```

### 13.7 序列圖

#### 13.7.1 結帳時優惠券驗證流程

```mermaid
sequenceDiagram
    participant U as 使用者
    participant FE as React 前端
    participant CC as CouponController
    participant CS as CouponService
    participant DB as PostgreSQL

    U->>FE: 輸入優惠券代碼
    FE->>CC: POST /api/coupons/validate {code, orderAmount}
    CC->>CS: validateCoupon(code, orderAmount)
    CS->>DB: SELECT * FROM coupon WHERE code = ?
    DB-->>CS: Coupon Entity
    CS->>CS: 驗證啟用/有效期/門檻/次數
    alt 驗證失敗
        CS-->>CC: 拋出例外
        CC-->>FE: 400 {message: "錯誤原因"}
        FE-->>U: 顯示錯誤訊息
    else 驗證通過
        CS->>CS: 計算折扣金額（伺服器）
        CS-->>CC: CouponValidateResponse
        CC-->>FE: 200 {discountAmount, finalAmount}
        FE-->>U: 顯示折扣金額
    end
```

#### 13.7.2 訂單成立後優惠券消耗流程

```mermaid
sequenceDiagram
    participant U as 使用者
    participant FE as React 前端
    participant CartC as CartController
    participant CartS as CartService
    participant CS as CouponService
    participant DB as PostgreSQL

    U->>FE: 送出結帳（含 couponCode）
    FE->>CartC: POST /api/cart/checkout {recipient, couponCode}
    CartC->>CartS: checkout(sessionId, recipient, couponCode)
    CartS->>DB: BEGIN TRANSACTION
    CartS->>DB: SELECT ... FOR UPDATE 各商品
    CartS->>CartS: 驗證庫存、扣減庫存
    alt 攜帶 couponCode
        CartS->>CS: validateCoupon(couponCode, orderAmount)
        CS->>DB: SELECT coupon
        CS-->>CartS: 驗證通過 + 折扣金額
        CartS->>CS: consumeCoupon(coupon, cartId, sessionId, discountAmount)
        CS->>DB: INSERT coupon_usage
        CS->>DB: UPDATE coupon.usage_count += 1
    end
    CartS->>DB: UPDATE cart.checked_out_at = now()
    CartS->>DB: COMMIT
    CartS-->>CartC: OrderSummary (含 discountAmount, finalTotal)
    CartC-->>FE: 200 訂單摘要
    FE-->>U: 顯示結帳成功
```

### 13.8 ER 圖（含優惠券）

```mermaid
erDiagram
    PRODUCT ||--o{ CART_ITEM : "被加入"
    CART ||--o{ CART_ITEM : "包含"
    COUPON ||--o{ COUPON_USAGE : "被使用"
    CART ||--o| COUPON_USAGE : "對應"

    PRODUCT {
        uuid id PK
        varchar name
        text description
        numeric price
        int stock
        text image_url
        timestamptz created_at
    }
    CART {
        uuid id PK
        varchar session_id
        timestamptz created_at
        timestamptz checked_out_at
    }
    CART_ITEM {
        uuid id PK
        uuid cart_id FK
        uuid product_id FK
        int quantity
        numeric unit_price
    }
    COUPON {
        uuid id PK
        varchar code UK
        varchar name
        varchar discount_type
        numeric discount_value
        numeric min_order_amount
        int max_usage_count
        int usage_count
        timestamptz start_date
        timestamptz end_date
        boolean is_active
        text description
        timestamptz created_at
    }
    COUPON_USAGE {
        uuid id PK
        uuid coupon_id FK
        uuid cart_id FK
        varchar session_id
        numeric discount_amount
        timestamptz used_at
    }
```

### 13.9 類別圖（優惠券相關）

```mermaid
classDiagram
    direction LR

    class DiscountType {
        <<enumeration>>
        PERCENTAGE
        FIXED
    }

    class Coupon {
        +UUID id
        +String code
        +String name
        +DiscountType discountType
        +BigDecimal discountValue
        +BigDecimal minOrderAmount
        +Integer maxUsageCount
        +Integer usageCount
        +Instant startDate
        +Instant endDate
        +Boolean isActive
        +String description
        +Instant createdAt
    }

    class CouponUsage {
        +UUID id
        +Coupon coupon
        +UUID cartId
        +String sessionId
        +BigDecimal discountAmount
        +Instant usedAt
    }

    class CouponValidateRequest {
        +String code
        +BigDecimal orderAmount
    }

    class CouponValidateResponse {
        +String code
        +String name
        +String description
        +String discountType
        +BigDecimal discountValue
        +BigDecimal discountAmount
        +BigDecimal originalAmount
        +BigDecimal finalAmount
    }

    class CouponController {
        +validateCoupon(CouponValidateRequest) CouponValidateResponse
        +getAvailableCoupons() List~CouponValidateResponse~
        +createCoupon(CouponCreateRequest) Coupon
        +updateCoupon(UUID, CouponCreateRequest) Coupon
        +deleteCoupon(UUID) void
        +listAllCoupons() List~Coupon~
    }

    class CouponService {
        +validateCoupon(code, orderAmount) CouponValidateResponse
        +calculateDiscount(coupon, orderAmount) BigDecimal
        +consumeCoupon(coupon, cartId, sessionId, discountAmount) void
        +releaseCoupon(cartId) void
        +getActiveCoupons() List~Coupon~
    }

    class CouponRepository {
        <<interface>>
        +findByCode(String) Optional~Coupon~
        +findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(Instant, Instant) List~Coupon~
    }

    class CouponUsageRepository {
        <<interface>>
        +findByCartId(UUID) Optional~CouponUsage~
        +existsByCouponIdAndSessionId(UUID, String) boolean
    }

    Coupon --> DiscountType
    CouponUsage --> Coupon
    CouponController ..> CouponService
    CouponController ..> CouponValidateRequest
    CouponController ..> CouponValidateResponse
    CouponService ..> CouponRepository
    CouponService ..> CouponUsageRepository
    CouponRepository ..> Coupon
    CouponUsageRepository ..> CouponUsage
```
