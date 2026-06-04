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
