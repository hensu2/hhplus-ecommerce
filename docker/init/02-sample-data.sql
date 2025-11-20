-- Sample Data for ecommerce database
SET NAMES utf8mb4;

USE ecommerce;

-- ===================================
-- 1. 샘플 사용자 데이터
-- ===================================
INSERT INTO users (username, point, role, created_at, updated_at) VALUES
('user1', 100000, 'USER', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('user2', 50000, 'USER', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('admin', 1000000, 'ADMIN', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- ===================================
-- 2. 샘플 상품 데이터
-- ===================================
INSERT INTO products (created_user_id, product_name, content, price, created_at, updated_at) VALUES
(3, '아이폰 15 Pro', '최신 아이폰 15 Pro', 1500000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(3, '갤럭시 S24', '삼성 갤럭시 S24', 1200000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(3, '맥북 Pro 14인치', 'M3 Pro 칩 탑재', 2500000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(3, '에어팟 Pro 2세대', '공간 음향 지원', 350000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(3, '애플워치 Series 9', '최신 애플워치', 550000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- ===================================
-- 3. 샘플 상품 옵션 데이터
-- ===================================
INSERT INTO product_options (product_id, option_type, additional_price, stock, created_at, updated_at) VALUES
-- 아이폰 15 Pro 옵션
(1, '색상:블랙,용량:256GB', 0, 100, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(1, '색상:화이트,용량:256GB', 0, 50, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(1, '색상:블랙,용량:512GB', 200000, 30, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
-- 갤럭시 S24 옵션
(2, '색상:그레이,용량:256GB', 0, 80, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(2, '색상:퍼플,용량:512GB', 150000, 40, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
-- 맥북 Pro 옵션
(3, '색상:스페이스그레이,메모리:18GB', 0, 20, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(3, '색상:실버,메모리:36GB', 500000, 10, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
-- 에어팟 Pro 옵션
(4, '기본', 0, 200, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
-- 애플워치 옵션
(5, '사이즈:41mm', 0, 60, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
(5, '사이즈:45mm', 50000, 40, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- ===================================
-- 4. 샘플 쿠폰 데이터
-- ===================================
INSERT INTO coupons (coupon_name, discount_type, discount_amount, use_min_amount, use_max_amount, stock, valid_from, valid_until, created_at, updated_at) VALUES
('신규 회원 10% 할인', 'PERCENT', 10, 100000, 50000, 100, UNIX_TIMESTAMP() * 1000, (UNIX_TIMESTAMP() + 2592000) * 1000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('5만원 할인 쿠폰', 'AMOUNT', 50000, 300000, 50000, 50, UNIX_TIMESTAMP() * 1000, (UNIX_TIMESTAMP() + 2592000) * 1000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('선착순 100명 15% 할인', 'PERCENT', 15, 200000, 100000, 100, UNIX_TIMESTAMP() * 1000, (UNIX_TIMESTAMP() + 2592000) * 1000, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- ===================================
-- 5. 샘플 상품 통계 데이터
-- ===================================
INSERT INTO product_statistics (product_id, view_count, sales_count, updated_at) VALUES
(1, 1500, 45, UNIX_TIMESTAMP() * 1000),
(2, 1200, 38, UNIX_TIMESTAMP() * 1000),
(3, 800, 22, UNIX_TIMESTAMP() * 1000),
(4, 2000, 120, UNIX_TIMESTAMP() * 1000),
(5, 900, 35, UNIX_TIMESTAMP() * 1000);
