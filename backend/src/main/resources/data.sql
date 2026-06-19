-- Seed data — ON CONFLICT DO NOTHING ensures idempotent inserts on restart

-- Product 1: Scandinavian Sofa
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000001',
    'Scandinavian Sofa',
    'Clean lines, solid oak legs, deep-seated comfort. Available in oatmeal and charcoal.',
    899.00,
    15,
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCuWd0wtbo1x9gEYuPI1iv_e58FD_ME7eXXSTiLZPwIsXJGbUsLde0c6KuEhjYEN5flxnHSV7NMoxDXbm9tXgJUAnqd9lHFjM-DlBVg5EmWMXDwtHVEYkU0DWPpiB_4SePfoEx_dVJGGuJ4IFo13FVc0nteITlAzDzBznyoeFn2YgdBWHUc6wT2MZ7vjcnETDi9Daxk-b5JbUKN8XS1MK6q-lKOR0BWaPkbbRXR_BpcEXFjatpxiwWoHWJHzpj6tunq7m-qJGwv7VV6'
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

-- 新增商品資料（2026-06-14）

-- Product 6: Linen Dining Chair
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000006',
    'Linen Dining Chair',
    'Natural linen seat pad, solid beech frame, stackable design. Suits any dining room aesthetic.',
    320.00,
    18,
    'https://images.unsplash.com/photo-1581539250439-c96689b516dd?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 7: Marble Side Table
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000007',
    'Marble Side Table',
    'White Carrara marble top, brushed gold base. A refined accent piece for bedroom or lounge.',
    670.00,
    10,
    'https://lh3.googleusercontent.com/aida-public/AB6AXuDZRztqNLYFFqq6y7zIgZku-EZ7PUjTtQX69s5hqZSWsZVh6d5iX-_wnT6_uBffy1bopU2HEjvkc_jtMr30yqSKgp5DpBOS6TbkiFXQtKJbeh3JGeeE4EMEL-nXZJQ3b44ku5alMem23-_nuoY7Q6Ef1tn6d4wana9Rcu8vF3xZirvOuRELZYBjefl8Vtb_GpHAqjET4anOkFchWOSjXEU2zM6_ky0I4W6xYdjg-FurFleg-8Y9iFyGHcKdrbRJSFe2_MM1iKTUzOYG'
)
ON CONFLICT DO NOTHING;


-- Product 9: Upholstered Bed Frame
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000009',
    'Upholstered Bed Frame',
    'Tufted headboard in dove-grey fabric, solid pine slat base. Fits queen and king mattresses.',
    1100.00,
    6,
    'https://lh3.googleusercontent.com/aida-public/AB6AXuC0QwV-vR57wcprGL_ARjxAQC3kNj17liHBa6AAPR1FH944VJKDa2afW1LNVvuujkw-53_7LAd9NvIQ2oTvTs30qgbejPQUDV_xYvmoVkHrkK6nxiZvSXI8P8V6BQRuaQAr01NxuyhSuBBdVBQBimgZlBNahs1bvf1TCB3t4MJJ-avVSFrhAQd1W9JbSv4L9n05y5L1EE0keqwzr8qx0Cm_c4pSThZNK11RUAI9NH4a3ScvBD9qcJE2HtOqEg_M8xjAUB9YXnXnxOUL'
)
ON CONFLICT DO NOTHING;

-- Product 10: Floating Wall Shelf
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000010',
    'Floating Wall Shelf',
    'Solid ash wood, concealed bracket mounting. Ideal for displaying books, candles, and small plants.',
    180.00,
    25,
    'https://images.unsplash.com/photo-1532372320572-cda25653a26d?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 11: Velvet Ottoman
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000011',
    'Velvet Ottoman',
    'Deep teal velvet, removable tray top, hidden storage inside. Doubles as a coffee table.',
    420.00,
    9,
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBJzT0NJUHTpxesQHfaV-Lqv7lsFuAyBOdR1ndKHhjXQ5_8L1LphbW_AsAiBlUcSgT37iBrfxek8_O_6MWkUywVJvD0zTpYHCpzl8HEXhK8zCN8u__g7GFLEehavMPGCjkD0d4-5FPIDdZ7KCfdX-NTotLWCtT747kmH8z5MCcs9WW5b3H5TQCtIhsp9ciJPAcVJoD5cKIhiAipJUVRnRZiFSsD1m0CvqHbvyf70HEnT_Mm1KLJ8RSHUSbYr_tsOwPPcCGJGOUtZjp9'
)
ON CONFLICT DO NOTHING;

-- Product 12: Industrial Desk Lamp
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000012',
    'Industrial Desk Lamp',
    'Matte black steel arm, Edison bulb included, rotary dimmer switch. Built for long work sessions.',
    210.00,
    20,
    'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 13: Concrete Planter Pot
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000013',
    'Concrete Planter Pot',
    'Hand-cast concrete, drainage hole with bamboo tray. Works indoors or on a sheltered balcony.',
    95.00,
    30,
    'https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 14: Extendable Dining Table
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000014',
    'Extendable Dining Table',
    'White oak veneer top extends from 160 cm to 240 cm. Seats four to eight with ease.',
    1350.00,
    4,
    'https://images.unsplash.com/photo-1549497538-303791108f95?w=600&auto=format&fit=crop'
)
ON CONFLICT DO NOTHING;

-- Product 15: Woven Storage Basket
INSERT INTO product (id, name, description, price, stock, image_url)
VALUES (
    '11111111-0000-0000-0000-000000000015',
    'Woven Storage Basket',
    'Seagrass weave, cotton rope handles, collapsible when empty. Keeps blankets and toys in check.',
    75.00,
    40,
    'https://images.unsplash.com/photo-1595515106969-1ce29566ff1c?w=600&auto=format&fit=crop'
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


