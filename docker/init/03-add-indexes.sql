-- Add Essential Indexes for Performance Optimization
-- Description: 성능 최적화를 위한 필수 인덱스 추가

SET NAMES utf8mb4;

USE ecommerce;

-- ===================================
-- 새로 추가되는 인덱스 (기존 스키마에 없던 것들)
-- ===================================

-- 1. cart.product_option_id 인덱스
CREATE INDEX idx_cart_product_option_id ON cart(product_option_id);

-- 2. products.created_user_id 인덱스
CREATE INDEX idx_products_created_user_id ON products(created_user_id);

-- 3. order_items.product_option_id 인덱스
CREATE INDEX idx_order_items_product_option_id ON order_items(product_option_id);

-- ===================================
-- 복합 인덱스 (조회 성능 향상)
-- ===================================

-- 4. orders 테이블: user_id + status 복합 인덱스 (사용자별 주문 상태 조회)
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

-- 5. coupon_history 테이블: user_id + status 복합 인덱스 (사용자별 쿠폰 상태 조회)
CREATE INDEX idx_coupon_history_user_status ON coupon_history(user_id, status);

-- 6. payments 테이블: user_id + status 복합 인덱스 (사용자별 결제 상태 조회)
CREATE INDEX idx_payments_user_status ON payments(user_id, status);

-- 7. orders 테이블: coupon_history_id 인덱스 (쿠폰 사용 주문 조회)
CREATE INDEX idx_orders_coupon_history_id ON orders(coupon_history_id);

-- 8. cart 테이블: user_id + created_at 복합 인덱스 (최신 장바구니 조회)
CREATE INDEX idx_cart_user_created ON cart(user_id, created_at);

-- 9. product_statistics 테이블: 판매량+조회수 복합 인덱스 (인기 상품 조회)
CREATE INDEX idx_product_stats_sales_views ON product_statistics(sales_count DESC, view_count DESC);
