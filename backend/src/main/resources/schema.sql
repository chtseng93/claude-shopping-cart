-- 啟用 pgcrypto 擴充套件以支援 gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── 商品資料表 ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    stock       INTEGER       NOT NULL CHECK (stock >= 0),
    image_url   TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── 購物車資料表 ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cart (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id     VARCHAR(255) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    checked_out_at TIMESTAMPTZ
);

-- session_id 查詢索引，加速訪客購物車查找
CREATE INDEX IF NOT EXISTS idx_cart_session_id ON cart(session_id);

-- ── 購物車明細資料表 ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cart_item (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id    UUID          NOT NULL REFERENCES cart(id),
    product_id UUID          NOT NULL REFERENCES product(id),
    quantity   INTEGER       NOT NULL CHECK (quantity >= 1),
    unit_price NUMERIC(10,2) NOT NULL,
    UNIQUE (cart_id, product_id)
);

-- ── 優惠券主檔資料表 ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupon (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50)   NOT NULL UNIQUE,
    name             VARCHAR(255)  NOT NULL,
    discount_type    VARCHAR(20)   NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    discount_value   NUMERIC(10,2) NOT NULL CHECK (discount_value > 0),
    min_order_amount NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (min_order_amount >= 0),
    max_usage_count  INTEGER       CHECK (max_usage_count > 0),
    usage_count      INTEGER       NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    start_date       TIMESTAMPTZ   NOT NULL,
    end_date         TIMESTAMPTZ   NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT true,
    description      TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- 優惠券代碼查詢索引（大寫，加速查找）
CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupon(code);

-- ── 優惠券使用記錄資料表 ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupon_usage (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id       UUID          NOT NULL REFERENCES coupon(id),
    cart_id         UUID          NOT NULL REFERENCES cart(id),
    session_id      VARCHAR(255)  NOT NULL,
    discount_amount NUMERIC(10,2) NOT NULL CHECK (discount_amount >= 0),
    used_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);
