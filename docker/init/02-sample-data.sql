-- Sample Data for ecommerce database (Medium Scale - for Concurrency Testing)
-- Target: 10K users, 2K products, 50K orders
-- Direct INSERT approach for reliability
SET NAMES utf8mb4;

USE ecommerce;

SET @now_ts = UNIX_TIMESTAMP() * 1000;

-- ===================================
-- 1. 사용자 데이터 (10,001명)
-- ===================================
SELECT '1/12: 사용자 생성 중...' AS progress;

-- Admin 사용자
INSERT INTO users (username, point, role, created_at, updated_at) VALUES
('admin', 10000000, 'ADMIN', @now_ts, @now_ts);

-- 일반 사용자 10,000명 (배치로 삽입)
INSERT INTO users (username, point, role, created_at, updated_at)
SELECT
    CONCAT('user', n),
    FLOOR(10000 + RAND() * 990000),
    'USER',
    @now_ts - FLOOR(RAND() * 31536000000),
    @now_ts
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d
) numbers
WHERE n <= 10000;

SELECT CONCAT('✓ ', COUNT(*), '명 생성 완료') FROM users;

-- ===================================
-- 2. 상품 데이터 (2,000개)
-- ===================================
SELECT '2/12: 상품 생성 중...' AS progress;

INSERT INTO products (created_user_id, product_name, content, price, created_at, updated_at)
SELECT
    1,  -- admin
    CONCAT(
        ELT(MOD(n, 20) + 1, 'iPhone', 'Galaxy', 'MacBook', 'iPad', 'AirPods', 'Watch', 'Tablet', 'Laptop', 'Monitor', 'Keyboard',
            'Mouse', 'Headphone', 'Speaker', 'Camera', 'Drone', 'TV', 'Refrigerator', 'Washer', 'Vacuum', 'AirConditioner'),
        ' ',
        ELT(MOD(n, 10) + 1, 'Pro', 'Plus', 'Ultra', 'Max', 'Mini', 'Lite', 'Standard', 'Premium', 'Basic', 'Deluxe'),
        ' ',
        n
    ),
    CONCAT('고품질 상품 #', n),
    FLOOR(10000 + RAND() * 2990000),
    @now_ts - FLOOR(RAND() * 31536000000),
    @now_ts
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d
) numbers
WHERE n <= 2000;

SELECT CONCAT('✓ ', COUNT(*), '개 생성 완료') FROM products;

-- ===================================
-- 3. 상품 옵션 데이터 (약 5,000개 - 상품당 2-3개)
-- ===================================
SELECT '3/12: 상품 옵션 생성 중...' AS progress;

-- 각 상품마다 2-3개의 옵션 생성
INSERT INTO product_options (product_id, option_type, additional_price, stock, created_at, updated_at)
SELECT
    p.id,
    CASE opt_num
        WHEN 1 THEN '색상:블랙,용량:256GB'
        WHEN 2 THEN '색상:화이트,용량:512GB'
        WHEN 3 THEN '색상:블루,용량:1TB'
    END,
    (opt_num - 1) * 100000,
    FLOOR(50 + RAND() * 450),
    @now_ts,
    @now_ts
FROM
    products p
CROSS JOIN
    (SELECT 1 AS opt_num UNION SELECT 2 UNION SELECT 3) opts
WHERE opt_num <= 2 + FLOOR(RAND() * 2);

SELECT CONCAT('✓ ', COUNT(*), '개 생성 완료') FROM product_options;

-- ===================================
-- 4. 쿠폰 데이터 (20개)
-- ===================================
SELECT '4/12: 쿠폰 생성 중...' AS progress;

INSERT INTO coupons (coupon_name, discount_type, discount_amount, use_min_amount, use_max_amount, stock, valid_from, valid_until, created_at, updated_at)
SELECT
    CONCAT(
        ELT(MOD(n, 3) + 1, '신규회원', '일반회원', 'VIP'),
        ' ',
        ELT(MOD(n, 2) + 1, '할인쿠폰', '특가쿠폰'),
        ' #', n
    ),
    IF(MOD(n, 2) = 0, 'PERCENT', 'AMOUNT'),
    IF(MOD(n, 2) = 0, 5 + MOD(n, 5) * 5, 10000 + MOD(n, 5) * 10000),
    50000 + (n * 10000),
    20000 + (n * 5000),
    1000 + FLOOR(RAND() * 4000),
    @now_ts,
    @now_ts + 2592000000,
    @now_ts,
    @now_ts
FROM (
    SELECT a.n + b.n * 10 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1) b
) numbers
WHERE n <= 20;

SELECT CONCAT('✓ ', COUNT(*), '개 생성 완료') FROM coupons;

-- ===================================
-- 5. 쿠폰 발급 내역 (30,000건)
-- ===================================
SELECT '5/12: 쿠폰 발급 내역 생성 중...' AS progress;

INSERT IGNORE INTO coupon_history (coupon_id, user_id, status, issued_at, used_at)
SELECT
    FLOOR(1 + RAND() * 20),
    FLOOR(1 + RAND() * 10001),
    IF(RAND() < 0.3, 'USED', 'ISSUED'),
    @now_ts - FLOOR(RAND() * 2592000000),
    IF(RAND() < 0.3, @now_ts - FLOOR(RAND() * 1296000000), NULL)
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + e.n * 10000 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2) e
) numbers
WHERE n <= 30000;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM coupon_history;

-- ===================================
-- 6. 포인트 내역 (100,000건)
-- ===================================
SELECT '6/12: 포인트 내역 생성 중...' AS progress;

INSERT INTO point_history (user_id, amount, transaction_type, description, created_at, updated_at)
SELECT
    FLOOR(1 + RAND() * 10001),
    CASE
        WHEN RAND() < 0.5 THEN FLOOR(1000 + RAND() * 99000)
        ELSE -FLOOR(1000 + RAND() * 49000)
    END,
    ELT(FLOOR(1 + RAND() * 4), 'CHARGE', 'USE', 'REFUND', 'REWARD'),
    ELT(FLOOR(1 + RAND() * 4), '포인트 충전', '주문 결제', '주문 취소 환불', '이벤트 리워드'),
    @now_ts - FLOOR(RAND() * 7776000000),
    @now_ts
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + e.n * 10000 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) e
) numbers
WHERE n <= 100000;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM point_history;

-- ===================================
-- 7. 장바구니 (5,000건)
-- ===================================
SELECT '7/12: 장바구니 생성 중...' AS progress;

INSERT INTO cart (user_id, product_id, product_option_id, quantity, created_at, updated_at)
SELECT
    FLOOR(1 + RAND() * 10001),
    FLOOR(1 + RAND() * 2000),
    FLOOR(1 + (SELECT COUNT(*) FROM product_options) * RAND()),
    FLOOR(1 + RAND() * 5),
    @now_ts - FLOOR(RAND() * 604800000),
    @now_ts
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS n) d
) numbers
WHERE n <= 5000;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM cart;

-- ===================================
-- 8. 주문 (50,000건)
-- ===================================
SELECT '8/12: 주문 생성 중...' AS progress;

INSERT INTO orders (user_id, status, coupon_history_id, discount_amount, point_discount, cancel_reason, ordered_at, created_at, updated_at)
SELECT
    FLOOR(1 + RAND() * 10001),
    ELT(FLOOR(1 + RAND() * 10), 'PENDING', 'CANCELLED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED'),
    NULL,
    IF(RAND() < 0.2, FLOOR(5000 + RAND() * 45000), 0),
    IF(RAND() < 0.3, FLOOR(1000 + RAND() * 19000), 0),
    IF(RAND() < 0.1, '고객 변심', NULL),
    @now_ts - FLOOR(RAND() * 15552000000),
    @now_ts - FLOOR(RAND() * 15552000000),
    @now_ts
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + e.n * 10000 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) e
) numbers
WHERE n <= 50000;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM orders;

-- ===================================
-- 9. 주문 상품 (100,000건 - 주문당 평균 2개)
-- ===================================
SELECT '9/12: 주문 상품 생성 중...' AS progress;

INSERT INTO order_items (order_id, product_id, product_option_id, product_name, option_type, quantity, price, created_at)
SELECT
    o.id,
    FLOOR(1 + RAND() * 2000),
    FLOOR(1 + (SELECT COUNT(*) FROM product_options) * RAND()),
    CONCAT('Product #', FLOOR(1 + RAND() * 2000)),
    '색상:블랙,용량:256GB',
    FLOOR(1 + RAND() * 3),
    FLOOR(10000 + RAND() * 990000),
    @now_ts
FROM orders o
CROSS JOIN (SELECT 1 AS item_num UNION SELECT 2) items
WHERE RAND() < 0.6;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM order_items;

-- ===================================
-- 10. 결제 (50,000건)
-- ===================================
SELECT '10/12: 결제 생성 중...' AS progress;

INSERT INTO payments (order_id, user_id, amount, status, created_at, updated_at)
SELECT
    o.id,
    o.user_id,
    FLOOR(10000 + RAND() * 2990000),
    CASE FLOOR(RAND() * 10)
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'FAILED'
        WHEN 2 THEN 'CANCELLED'
        ELSE 'COMPLETED'
    END,
    @now_ts - FLOOR(RAND() * 15552000000),
    @now_ts
FROM orders o;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM payments;

-- ===================================
-- 11. 상품 통계 (2,000개)
-- ===================================
SELECT '11/12: 상품 통계 생성 중...' AS progress;

INSERT INTO product_statistics (product_id, view_count, sales_count, updated_at)
SELECT
    p.id,
    FLOOR(100 + RAND() * 9900),
    FLOOR(10 + RAND() * 490),
    @now_ts
FROM products p;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM product_statistics;

-- ===================================
-- 12. 외부 동기화 로그 (10,000건)
-- ===================================
SELECT '12/12: 외부 동기화 로그 생성 중...' AS progress;

INSERT INTO external_sync_log (order_id, status, retry_count, error_message, sent_at, created_at, updated_at)
SELECT
    FLOOR(1 + RAND() * 50000),
    ELT(FLOOR(1 + RAND() * 10), 'PENDING', 'FAILED', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS'),
    FLOOR(RAND() * 4),
    IF(RAND() < 0.1, 'Connection timeout', NULL),
    IF(RAND() < 0.9, @now_ts - FLOOR(RAND() * 7776000000), NULL),
    @now_ts - FLOOR(RAND() * 7776000000),
    @now_ts
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + 1 AS n
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS n) d
) numbers
WHERE n <= 10000;

SELECT CONCAT('✓ ', COUNT(*), '건 생성 완료') FROM external_sync_log;

-- ===================================
-- 생성 완료 및 통계
-- ===================================
SELECT '========================================' AS '';
SELECT '✅ 데이터 생성 완료!' AS '';
SELECT '========================================' AS '';

SELECT 'users' AS table_name, COUNT(*) AS record_count FROM users
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'product_options', COUNT(*) FROM product_options
UNION ALL
SELECT 'coupons', COUNT(*) FROM coupons
UNION ALL
SELECT 'coupon_history', COUNT(*) FROM coupon_history
UNION ALL
SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL
SELECT 'cart', COUNT(*) FROM cart
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'payments', COUNT(*) FROM payments
UNION ALL
SELECT 'product_statistics', COUNT(*) FROM product_statistics
UNION ALL
SELECT 'external_sync_log', COUNT(*) FROM external_sync_log
UNION ALL
SELECT '========================================', '========================================'
UNION ALL
SELECT '총 레코드', COUNT(*) FROM (
    SELECT 1 FROM users UNION ALL
    SELECT 1 FROM products UNION ALL
    SELECT 1 FROM product_options UNION ALL
    SELECT 1 FROM coupons UNION ALL
    SELECT 1 FROM coupon_history UNION ALL
    SELECT 1 FROM point_history UNION ALL
    SELECT 1 FROM cart UNION ALL
    SELECT 1 FROM orders UNION ALL
    SELECT 1 FROM order_items UNION ALL
    SELECT 1 FROM payments UNION ALL
    SELECT 1 FROM product_statistics UNION ALL
    SELECT 1 FROM external_sync_log
) total;

SELECT '========================================' AS '';