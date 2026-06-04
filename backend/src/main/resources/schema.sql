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
