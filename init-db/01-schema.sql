-- Database Schema for E-commerce Platform
-- Based on data-models.md

-- Create Database
CREATE DATABASE IF NOT EXISTS ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommerce;

-- ==========================================
-- Table: USERS
-- ==========================================
CREATE TABLE USERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    point BIGINT NOT NULL DEFAULT 0,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: POINT_HISTORY
-- ==========================================
CREATE TABLE POINT_HISTORY (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL COMMENT 'EARN, USE, REFUND',
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USERS(id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: PRODUCT
-- ==========================================
CREATE TABLE PRODUCT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_user_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    content TEXT,
    price BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_user_id) REFERENCES USERS(id),
    INDEX idx_created_user_id (created_user_id),
    INDEX idx_product_name (product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: PRODUCT_OPTIONS
-- ==========================================
CREATE TABLE PRODUCT_OPTIONS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    option_type VARCHAR(100) NOT NULL,
    additional_price BIGINT NOT NULL DEFAULT 0,
    stock BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES PRODUCT(id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: COUPONS
-- ==========================================
CREATE TABLE COUPONS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL COMMENT 'PERCENTAGE, FIXED',
    discount_amount BIGINT NOT NULL,
    use_min_amount BIGINT NOT NULL DEFAULT 0,
    use_max_amount BIGINT NOT NULL DEFAULT 0,
    stock BIGINT NOT NULL DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_validity (valid_from, valid_until),
    INDEX idx_stock (stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: COUPON_HISTORY
-- ==========================================
CREATE TABLE COUPON_HISTORY (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'ISSUED, USED, EXPIRED',
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (coupon_id) REFERENCES COUPONS(id),
    FOREIGN KEY (user_id) REFERENCES USERS(id),
    INDEX idx_coupon_id (coupon_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: CART
-- ==========================================
CREATE TABLE CART (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USERS(id),
    FOREIGN KEY (product_id) REFERENCES PRODUCT(id),
    FOREIGN KEY (product_option_id) REFERENCES PRODUCT_OPTIONS(id),
    INDEX idx_user_id (user_id),
    INDEX idx_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: ORDERS
-- ==========================================
CREATE TABLE ORDERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, CONFIRMED, CANCELLED',
    coupon_history_id BIGINT,
    discount_amount BIGINT NOT NULL DEFAULT 0 COMMENT '쿠폰 할인액',
    point_discount BIGINT NOT NULL DEFAULT 0 COMMENT '포인트 사용액',
    cancel_reason VARCHAR(500),
    ordered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES USERS(id),
    FOREIGN KEY (coupon_history_id) REFERENCES COUPON_HISTORY(id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_ordered_at (ordered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: ORDER_ITEMS
-- ==========================================
CREATE TABLE ORDER_ITEMS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    unit_price BIGINT NOT NULL COMMENT '주문 당시의 가격 (이력 보존)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES ORDERS(id),
    FOREIGN KEY (product_id) REFERENCES PRODUCT(id),
    FOREIGN KEY (product_option_id) REFERENCES PRODUCT_OPTIONS(id),
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    INDEX idx_created_at (created_at, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: PAYMENTS
-- ==========================================
CREATE TABLE PAYMENTS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    payment_amount BIGINT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, COMPLETED, FAILED, REFUNDED',
    earned_point BIGINT NOT NULL DEFAULT 0,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES ORDERS(id),
    FOREIGN KEY (user_id) REFERENCES USERS(id),
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: EXTERNAL_SYNC_LOG
-- ==========================================
CREATE TABLE EXTERNAL_SYNC_LOG (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL COMMENT 'SUCCESS, FAILED, FAILED_PERMANENT',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES ORDERS(id),
    INDEX idx_order_id (order_id),
    INDEX idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Table: PRODUCT_STATISTICS
-- ==========================================
CREATE TABLE PRODUCT_STATISTICS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sales_count BIGINT NOT NULL DEFAULT 0,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES PRODUCT(id),
    INDEX idx_product_id (product_id),
    INDEX idx_period_sales (period_start, period_end, sales_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
