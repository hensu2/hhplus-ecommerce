# Database ERD

## 개요
이커머스 쇼핑몰 데이터베이스 설계 문서

---

## 📊 ERD Diagram

```dbml
Table USERS {
  id integer [primary key]
  username varchar
  point integer
  role varchar
  created_at timestamp
  updated_at timestamp
}

Table POINT_HISTORY {
  id integer [primary key]
  user_id integer
  amount integer
  Transaction_type varchar
  created_at timestamp
  updated_at timestamp
}

Table PRODECT {
  id integer [primary key]
  created_userid integer
  productName varchar
  content varchar
  price integer
  created_at timestamp
  updated_at timestamp
}

Table prodectOption{
  id integer [primary key]
  product_id integer
  option_type varchar
  additional_price varchar
  stock integer
  created_at timestamp
  updated_at timestamp
}

Table COUPONS {
  id integer [primary key]
  coupon_name varchar
  discount_type varchar // 퍼센트, 원
  discount_amount varchar
  use_min_amount integer
  use_max_amount integer
  stock integer //쿠폰 잔여량
  valid_from timestamp
  valid_until timestamp
  created_at timestamp
  updated_at timestamp
}

Table COUPONS_HISTORY {
  id integer [primary key]
  coupons_id integer
  user_id integer
  status varchar
  created_at timestamp
  updated_at timestamp
}

Table CART {
  id integer [primary key]
  user_id integer
  prodect_id integer
  prodect_option_id integer
  quantity integer
  created_at timestamp
  updated_at timestamp
}

Table ORDERS {
  id integer [primary key]
  user_id integer
  status varchar
  coupons_history_id integer
  total_amount integer
  discount_amount integer
  final_amount integer 
  ordered_at timestamp
  created_at timestamp
  updated_at timestamp
}

Table ORDER_ITEMS {
  id integer [primary key]
  order_id integer
  product_id integer
  prodect_option_id varchar
  quantity integer
  unit_price integer
  total_price integer
  created_at timestamp
}

Table PAYMENTS {
  id integer [primary key]
  order_id integer [unique]
  user_id integer
  payment_amount integer
  paid_at timestamp
  created_at timestamp
  updated_at timestamp
}

Table EXTERNAL_SYNC_LOG {
  id integer [primary key]
  order_id integer
  status varchar // SUCCESS, FAILED, FAILED_PERMANENT
  retry_count integer
  error_message text
  sent_at timestamp
  created_at timestamp
  updated_at timestamp
}

// USERS 관계
Ref: POINT_HISTORY.user_id > USERS.id
Ref: PRODECT.created_userid > USERS.id
Ref: COUPONS_HISTORY.user_id > USERS.id
Ref: CART.user_id > USERS.id
Ref: ORDERS.user_id > USERS.id
Ref: PAYMENTS.user_id > USERS.id

// PRODECT 관계
Ref: prodectOption.product_id > PRODECT.id
Ref: CART.prodect_id > PRODECT.id
Ref: ORDER_ITEMS.product_id > PRODECT.id

// prodectOption 관계
Ref: CART.prodect_option_id > prodectOption.id
Ref: ORDER_ITEMS.prodect_option_id > prodectOption.id

// COUPONS_HISTORY 관계
Ref: ORDERS.coupons_history_id > COUPONS_HISTORY.id

// ORDERS 관계
Ref: ORDER_ITEMS.order_id > ORDERS.id
Ref: PAYMENTS.order_id - ORDERS.id // 1:1 관계
Ref: EXTERNAL_SYNC_LOG.order_id > ORDERS.id

Ref: COUPONS.id < COUPONS_HISTORY.coupons_id
```

---

## 📋 테이블 설명

### 1. USERS (사용자)
사용자 정보를 관리하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 사용자 ID (PK) |
| username | varchar | 사용자명 |
| point | integer | 보유 포인트 |
| role | varchar | 권한 (USER, ADMIN) |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- POINT_HISTORY (1:N)
- PRODECT (1:N) - 상품 등록자
- COUPONS_HISTORY (1:N)
- CART (1:N)
- ORDERS (1:N)
- PAYMENTS (1:N)

---

### 2. POINT_HISTORY (포인트 내역)
포인트 적립/사용 내역을 기록하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 내역 ID (PK) |
| user_id | integer | 사용자 ID (FK) |
| amount | integer | 포인트 금액 |
| Transaction_type | varchar | 거래 타입 (EARN, USE, REFUND) |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- USERS (N:1)

---

### 3. PRODECT (상품)
상품 기본 정보를 저장하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 상품 ID (PK) |
| created_userid | integer | 등록자 ID (FK) |
| productName | varchar | 상품명 |
| content | varchar | 상품 설명 |
| price | integer | 기본 가격 |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- USERS (N:1) - 등록자
- prodectOption (1:N)
- CART (1:N)
- ORDER_ITEMS (1:N)

---

### 4. prodectOption (상품 옵션)
상품의 옵션 정보 및 재고를 관리하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 옵션 ID (PK) |
| product_id | integer | 상품 ID (FK) |
| option_type | varchar | 옵션 타입 (색상, 사이즈 등) |
| additional_price | varchar | 추가 가격 |
| stock | integer | 재고 수량 |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- PRODECT (N:1)
- CART (1:N)
- ORDER_ITEMS (1:N)

---

### 5. COUPONS (쿠폰)
쿠폰 정보를 관리하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 쿠폰 ID (PK) |
| coupon_name | varchar | 쿠폰명 |
| discount_type | varchar | 할인 타입 (PERCENT, AMOUNT) |
| discount_amount | varchar | 할인 금액/비율 |
| use_min_amount | integer | 최소 사용 금액 |
| use_max_amount | integer | 최대 할인 금액 |
| stock | integer | 쿠폰 잔여량 |
| valid_from | timestamp | 쿠폰 유효 시작일시 |
| valid_until | timestamp | 쿠폰 유효 종료일시 |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- COUPONS_HISTORY (1:N)

---

### 6. COUPONS_HISTORY (쿠폰 발급/사용 내역)
사용자별 쿠폰 발급 및 사용 내역을 관리하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 내역 ID (PK) |
| coupons_id | integer | 쿠폰 ID (FK) |
| user_id | integer | 사용자 ID (FK) |
| status | varchar | 상태 (ISSUED, USED, EXPIRED) |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- COUPONS (N:1)
- USERS (N:1)
- ORDERS (1:N)

---

### 7. CART (장바구니)
사용자의 장바구니 정보를 저장하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 장바구니 ID (PK) |
| user_id | integer | 사용자 ID (FK) |
| prodect_id | integer | 상품 ID (FK) |
| prodect_option_id | integer | 상품 옵션 ID (FK) |
| quantity | integer | 수량 |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- USERS (N:1)
- PRODECT (N:1)
- prodectOption (N:1)

---

### 8. ORDERS (주문)
주문 정보를 관리하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 주문 ID (PK) |
| user_id | integer | 사용자 ID (FK) |
| status | varchar | 주문 상태 (PENDING, PAID, CANCELLED) |
| coupons_history_id | integer | 쿠폰 사용 내역 ID (FK) |
| total_amount | integer | 총 주문 금액 |
| discount_amount | integer | 할인 금액 |
| final_amount | integer | 최종 결제 금액 |
| ordered_at | timestamp | 주문 완료 일시 |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- USERS (N:1)
- COUPONS_HISTORY (N:1)
- ORDER_ITEMS (1:N)
- PAYMENTS (1:1)

---

### 9. ORDER_ITEMS (주문 상품)
주문에 포함된 상품 상세 정보를 저장하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 주문상품 ID (PK) |
| order_id | integer | 주문 ID (FK) |
| product_id | integer | 상품 ID (FK) |
| prodect_option_id | varchar | 상품 옵션 ID (FK) |
| quantity | integer | 수량 |
| unit_price | integer | 단가 |
| total_price | integer | 총 금액 |
| created_at | timestamp | 생성일시 |

**관계:**
- ORDERS (N:1)
- PRODECT (N:1)
- prodectOption (N:1)

---

### 10. PAYMENTS (결제)
결제 정보를 관리하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 결제 ID (PK) |
| order_id | integer | 주문 ID (FK, UNIQUE) |
| user_id | integer | 사용자 ID (FK) |
| payment_amount | integer | 결제 금액 |
| paid_at | timestamp | 결제 완료 일시 |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- ORDERS (1:1)
- USERS (N:1)

---

### 11. EXTERNAL_SYNC_LOG (외부 동기화 로그)
주문 데이터의 외부 시스템 전송 내역을 기록하는 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | integer | 로그 ID (PK) |
| order_id | integer | 주문 ID (FK) |
| status | varchar | 전송 상태 (SUCCESS, FAILED, FAILED_PERMANENT) |
| retry_count | integer | 재시도 횟수 |
| error_message | text | 오류 메시지 |
| sent_at | timestamp | 전송 시도 일시 |
| created_at | timestamp | 생성일시 |
| updated_at | timestamp | 수정일시 |

**관계:**
- ORDERS (N:1)

**상태 설명:**
- SUCCESS: 전송 성공
- FAILED: 전송 실패 (재시도 가능)
- FAILED_PERMANENT: 재시도 횟수 초과로 영구 실패

---

## 🔗 관계도 요약

### USERS를 중심으로
```
USERS
├── POINT_HISTORY (1:N) - 포인트 내역
├── PRODECT (1:N) - 등록한 상품
├── COUPONS_HISTORY (1:N) - 발급받은 쿠폰
├── CART (1:N) - 장바구니
├── ORDERS (1:N) - 주문
└── PAYMENTS (1:N) - 결제
```

### PRODECT를 중심으로
```
PRODECT
├── prodectOption (1:N) - 상품 옵션
├── CART (1:N) - 장바구니 아이템
└── ORDER_ITEMS (1:N) - 주문 상품
```

### ORDERS를 중심으로
```
ORDERS
├── ORDER_ITEMS (1:N) - 주문 상품들
├── PAYMENTS (1:1) - 결제 정보
├── EXTERNAL_SYNC_LOG (1:N) - 외부 전송 로그
├── COUPONS_HISTORY (N:1) - 사용한 쿠폰
└── USERS (N:1) - 주문자
```

---

## 💡 주요 비즈니스 로직

### 1. 재고 관리
- `prodectOption.stock`에서 재고 관리
- 주문 생성시 재고 차감
- 주문 취소시 재고 복구

### 2. 포인트 관리
- `USERS.point`에 현재 포인트 저장
- `POINT_HISTORY`에 모든 포인트 내역 기록
- 거래 타입: EARN (적립), USE (사용), REFUND (환불)

### 3. 쿠폰 관리
- `COUPONS`에서 쿠폰 정의
- `COUPONS_HISTORY`에서 사용자별 발급/사용 관리
- 상태: ISSUED (발급), USED (사용), EXPIRED (만료)

### 4. 주문/결제 흐름
1. CART에서 상품 선택
2. ORDERS 생성 (PENDING)
3. ORDER_ITEMS에 상품 저장
4. 재고 차감 (prodectOption.stock)
5. PAYMENTS 생성
6. ORDERS 상태 → PAID
7. 외부 시스템에 주문 데이터 전송 (비동기)

### 5. 외부 데이터 전송
- 주문 완료 후 외부 시스템에 비동기 전송
- `EXTERNAL_SYNC_LOG`에 전송 내역 기록
- 실패 시 최대 3회 재시도
- 재시도 초과 시 FAILED_PERMANENT 상태로 기록
- 외부 전송 실패해도 주문은 정상 처리 유지

---

## 📌 인덱스 권장사항

```sql
-- 자주 조회되는 외래키에 인덱스
CREATE INDEX idx_point_history_user_id ON POINT_HISTORY(user_id);
CREATE INDEX idx_cart_user_id ON CART(user_id);
CREATE INDEX idx_orders_user_id ON ORDERS(user_id);
CREATE INDEX idx_order_items_order_id ON ORDER_ITEMS(order_id);
CREATE INDEX idx_payments_order_id ON PAYMENTS(order_id);
CREATE INDEX idx_external_sync_log_order_id ON EXTERNAL_SYNC_LOG(order_id);

-- 복합 인덱스
CREATE INDEX idx_cart_user_product ON CART(user_id, prodect_id);
CREATE INDEX idx_coupons_history_status ON COUPONS_HISTORY(user_id, status);
CREATE INDEX idx_external_sync_log_status ON EXTERNAL_SYNC_LOG(status, created_at);

-- 쿠폰 유효기간 조회용 인덱스
CREATE INDEX idx_coupons_validity ON COUPONS(valid_from, valid_until);
```

---

## 🚀 확장 가능성

### 추가 고려사항
- 상품 카테고리 테이블
- 배송 정보 테이블
- 리뷰/평점 테이블
- 위시리스트 테이블
- 주문 배송 추적 테이블
- 알림/푸시 메시지 테이블
- 외부 API 연동 설정 테이블

---

**작성일:** 2024-10-30
**최종 수정일:** 2024-10-30
**버전:** 1.1

**변경 이력:**
- v1.1 (2024-10-30): CART.quantity 추가, COUPONS 유효기간 필드 추가, EXTERNAL_SYNC_LOG 테이블 추가
- v1.0 (2024-10-30): 초기 작성