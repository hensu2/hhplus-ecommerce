-- Database: ecommerce
-- Description: 이커머스 쇼핑몰 데이터베이스 스키마

SET NAMES utf8mb4;

USE ecommerce;

-- ===================================
-- 1. USERS (사용자)
-- ===================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    point BIGINT NOT NULL DEFAULT 0,
    role VARCHAR(20) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_username (username),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 2. POINT_HISTORY (포인트 내역)
-- ===================================
CREATE TABLE IF NOT EXISTS point_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_user_created (user_id, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 3. PRODUCTS (상품)
-- ===================================
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_user_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    content TEXT,
    price BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_product_name (product_name),
    INDEX idx_created_at (created_at),
    INDEX idx_price (price),
    FOREIGN KEY (created_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 4. PRODUCT_OPTIONS (상품 옵션)
-- ===================================
CREATE TABLE IF NOT EXISTS product_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    option_type VARCHAR(100) NOT NULL,
    additional_price BIGINT NOT NULL DEFAULT 0,
    stock BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_product_id (product_id),
    INDEX idx_stock (stock),
    INDEX idx_product_stock (product_id, stock),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 5. COUPONS (쿠폰)
-- ===================================
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_amount INT NOT NULL,
    use_min_amount INT NOT NULL DEFAULT 0,
    use_max_amount INT NOT NULL DEFAULT 0,
    stock INT NOT NULL DEFAULT 0,
    valid_from BIGINT NOT NULL,
    valid_until BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_valid_period (valid_from, valid_until),
    INDEX idx_stock (stock),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 6. COUPON_HISTORY (쿠폰 발급 내역)
-- ===================================
CREATE TABLE IF NOT EXISTS coupon_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    issued_at BIGINT NOT NULL,
    used_at BIGINT,
    INDEX idx_user_id (user_id),
    INDEX idx_coupon_id (coupon_id),
    INDEX idx_user_coupon (user_id, coupon_id),
    INDEX idx_status (status),
    INDEX idx_issued_at (issued_at),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 7. CART (장바구니)
-- ===================================
CREATE TABLE IF NOT EXISTS cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_user_product (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (product_option_id) REFERENCES product_options(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 8. ORDERS (주문)
-- ===================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    coupon_history_id BIGINT,
    discount_amount INT NOT NULL DEFAULT 0,
    point_discount INT NOT NULL DEFAULT 0,
    cancel_reason VARCHAR(500),
    ordered_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_ordered_at (ordered_at),
    INDEX idx_user_ordered (user_id, ordered_at),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_history_id) REFERENCES coupon_history(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 9. ORDER_ITEMS (주문 상세)
-- ===================================
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    option_type VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL,
    created_at BIGINT NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (product_option_id) REFERENCES product_options(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 10. PAYMENTS (결제)
-- ===================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_order_id (order_id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================
-- 11. EXTERNAL_SYNC_LOG (외부 동기화 로그)
-- ===================================
CREATE TABLE IF NOT EXISTS external_sync_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    sent_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
