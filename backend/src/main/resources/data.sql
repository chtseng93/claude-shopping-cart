-- Seed data — ON CONFLICT DO NOTHING ensures idempotent inserts on restart

-- Product 1: Scandinavian Sofa
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000001',
    'Scandinavian Sofa',
    'Clean lines, solid oak legs, deep-seated comfort. Available in oatmeal and charcoal.',
    899.00,
    15,
    'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 2: Accent Armchair
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000002',
    'Accent Armchair',
    'Velvet upholstery, tapered walnut legs, perfect for a reading nook or living room corner.',
    1200.00,
    8,
    'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 3: Walnut Coffee Table
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000003',
    'Walnut Coffee Table',
    'Solid American walnut, round top with shelf below. A centerpiece for any living space.',
    450.00,
    20,
    'https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 4: Open Bookshelf (low stock)
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000004',
    'Open Bookshelf',
    '5-tier open shelving in matte black steel. Holds books, plants, and your favorite objects.',
    590.00,
    3,
    'https://images.unsplash.com/photo-1594026112284-02bb6f3352fe?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 5: Arc Floor Lamp (sold out — demonstrates stock=0 scenario)
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000005',
    'Arc Floor Lamp',
    'Brushed brass arc with a linen shade. Fills any corner with warm, sculptural light.',
    780.00,
    0,
    'https://images.unsplash.com/photo-1606425288528-4cebbfc69de7?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- ── 優惠券 Seed 資料 ───────────────────────────────────────────────────

-- 優惠券 1: 新會員 10% 折扣（長期有效，無次數限制）
INSERT INTO coupon (id, code, name, discount_type, discount_value, min_order_amount, max_usage_count, usage_count, start_date, end_date, is_active, description)
VALUES (
    'cccccccc-0000-0000-0000-000000000001',
    'WELCOME10',
    'New Member Offer',
    'PERCENTAGE',
    10,
    0,
    NULL,
    0,
    '2026-01-01T00:00:00Z',
    '2026-12-31T23:59:59Z',
    true,
    '10% off your first order. No minimum spend.'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 優惠券 2: 滿千折五百（固定金額，限量 50 份）
INSERT INTO coupon (id, code, name, discount_type, discount_value, min_order_amount, max_usage_count, usage_count, start_date, end_date, is_active, description)
VALUES (
    'cccccccc-0000-0000-0000-000000000002',
    'SAVE500',
    'Save NT$500',
    'FIXED',
    500,
    1000,
    50,
    0,
    '2026-06-01T00:00:00Z',
    '2026-09-30T23:59:59Z',
    true,
    'NT$500 off orders over NT$1,000. Limited to 50 uses.'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 優惠券 3: 夏季 20% 折扣（有次數限制，最低消費 500）
INSERT INTO coupon (id, code, name, discount_type, discount_value, min_order_amount, max_usage_count, usage_count, start_date, end_date, is_active, description)
VALUES (
    'cccccccc-0000-0000-0000-000000000003',
    'SUMMER20',
    'Summer Sale',
    'PERCENTAGE',
    20,
    500,
    30,
    0,
    '2026-07-01T00:00:00Z',
    '2026-08-31T23:59:59Z',
    true,
    '20% off sitewide. Min. spend NT$500. Limited to 30 uses.'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;
