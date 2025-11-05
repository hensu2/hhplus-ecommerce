# API Specification

## Base URL
```
http://localhost:8080
```

## Swagger UI
```
http://localhost:8080/swagger-ui.html
```

## API 문서
```
http://localhost:8080/api-docs
```

---

## 목차
1. [상품 관리 API](#1-상품-관리-api)
2. [장바구니 API](#2-장바구니-api)
3. [포인트 API](#3-포인트-api)
4. [쿠폰 API](#4-쿠폰-api)
5. [주문 API](#5-주문-api)
6. [결제 API](#6-결제-api)
7. [공통 응답 형식](#공통-응답-형식)

---

## 1. 상품 관리 API

### 1-1. 상품 목록 조회
상품 목록을 페이징하여 조회합니다.

**Endpoint:** `GET /api/products`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Integer | No | 페이지 번호 (default: 0) |
| size | Integer | No | 페이지 크기 (default: 20) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "productName": "상품명",
      "content": "상품 설명",
      "price": 10000,
      "createdAt": "2024-10-30T00:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

---

### 1-2. 상품 상세 조회
특정 상품의 상세 정보와 옵션, 재고를 조회합니다.

**Endpoint:** `GET /api/products/{id}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | 상품 ID |

**Response (200 OK):**
```json
{
  "id": 1,
  "productName": "상품명",
  "content": "상품 설명",
  "price": 10000,
  "options": [
    {
      "id": 1,
      "optionType": "색상:블랙",
      "additionalPrice": 0,
      "stock": 100
    },
    {
      "id": 2,
      "optionType": "색상:화이트",
      "additionalPrice": 1000,
      "stock": 50
    }
  ],
  "createdAt": "2024-10-30T00:00:00"
}
```

**Error Response (404 Not Found):**
```json
{
  "error": "NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

---

### 1-3. 인기 상품 조회
최근 3일간 판매량 기준 Top 5 상품을 조회합니다.

**Endpoint:** `GET /api/products/popular`

**Response (200 OK):**
```json
{
  "products": [
    {
      "id": 1,
      "productName": "인기 상품 1",
      "price": 10000,
      "salesCount": 150,
      "ranking": 1
    },
    {
      "id": 2,
      "productName": "인기 상품 2",
      "price": 20000,
      "salesCount": 120,
      "ranking": 2
    }
  ],
  "period": "최근 3일",
  "generatedAt": "2024-10-30T00:00:00"
}
```

---

## 2. 장바구니 API

### 2-1. 장바구니 조회
현재 사용자의 장바구니 목록을 조회합니다.

**Endpoint:** `GET /api/cart`

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "상품명",
      "optionId": 1,
      "optionType": "색상:블랙",
      "quantity": 2,
      "unitPrice": 10000,
      "totalPrice": 20000,
      "stock": 100
    }
  ],
  "totalAmount": 20000
}
```

---

### 2-2. 장바구니 추가
상품을 장바구니에 추가합니다. 재고를 확인합니다.

**Endpoint:** `POST /api/cart`

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "productId": 1,
  "optionId": 1,
  "quantity": 2
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "productId": 1,
  "productName": "상품명",
  "optionId": 1,
  "optionType": "색상:블랙",
  "quantity": 2,
  "unitPrice": 10000,
  "totalPrice": 20000,
  "addedAt": "2024-10-30T00:00:00"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "OUT_OF_STOCK",
  "message": "재고가 부족합니다.",
  "availableStock": 1
}
```

---

### 2-3. 장바구니 수량 변경
장바구니 아이템의 수량을 변경합니다.

**Endpoint:** `PUT /api/cart/{id}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | 장바구니 아이템 ID |

**Request Body:**
```json
{
  "quantity": 3
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "productId": 1,
  "quantity": 3,
  "totalPrice": 30000,
  "updatedAt": "2024-10-30T00:00:00"
}
```

---

### 2-4. 장바구니 삭제
장바구니에서 특정 아이템을 삭제합니다.

**Endpoint:** `DELETE /api/cart/{id}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | 장바구니 아이템 ID |

**Response (204 No Content):**
```
(empty response body)
```

---

## 3. 포인트 API

### 3-1. 포인트 조회
현재 사용자의 포인트 잔액을 조회합니다.

**Endpoint:** `GET /api/users/me/point`

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "userId": 1,
  "username": "user123",
  "point": 50000,
  "updatedAt": "2024-10-30T00:00:00"
}
```

---

### 3-2. 포인트 충전
사용자의 포인트를 충전합니다.

**Endpoint:** `POST /api/users/me/point/charge`

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "amount": 10000
}
```

**Response (200 OK):**
```json
{
  "userId": 1,
  "amount": 10000,
  "afterBalance": 60000,
  "transactionType": "EARN",
  "chargedAt": "2024-10-30T00:00:00"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "INVALID_AMOUNT",
  "message": "충전 금액은 1,000원 이상이어야 합니다."
}
```

---

### 3-3. 포인트 사용 이력 조회
포인트 충전/사용 이력을 조회합니다.

**Endpoint:** `GET /api/users/me/point/history`

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Integer | No | 페이지 번호 (default: 0) |
| size | Integer | No | 페이지 크기 (default: 20) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "amount": 10000,
      "transactionType": "EARN",
      "description": "포인트 충전",
      "createdAt": "2024-10-30T00:00:00"
    },
    {
      "id": 2,
      "amount": -5000,
      "transactionType": "USE",
      "description": "주문 결제",
      "createdAt": "2024-10-29T00:00:00"
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "number": 0
}
```

---

## 4. 쿠폰 API

### 4-1. 쿠폰 발급 (선착순)
선착순 쿠폰을 발급받습니다. 한정 수량이며, 동시성 제어가 적용됩니다.

**Endpoint:** `POST /api/coupons/{couponId}/issue`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| couponId | Long | Yes | 쿠폰 ID |

**Response (201 Created):**
```json
{
  "id": 1,
  "couponId": 1,
  "couponName": "신규 회원 10% 할인 쿠폰",
  "discountType": "PERCENT",
  "discountAmount": 10,
  "validFrom": "2024-10-30T00:00:00",
  "validUntil": "2024-11-30T23:59:59",
  "status": "ISSUED",
  "issuedAt": "2024-10-30T00:00:00"
}
```

**Error Response (400 Bad Request - 쿠폰 소진):**
```json
{
  "error": "OUT_OF_STOCK",
  "message": "쿠폰이 모두 소진되었습니다."
}
```

**Error Response (400 Bad Request - 이미 발급):**
```json
{
  "error": "ALREADY_ISSUED",
  "message": "이미 발급받은 쿠폰입니다."
}
```

**Error Response (429 Too Many Requests):**
```json
{
  "error": "TOO_MANY_REQUESTS",
  "message": "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
}
```

---

### 4-2. 내 쿠폰 조회
발급받은 쿠폰 목록을 조회합니다.

**Endpoint:** `GET /api/coupons/me`

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | String | No | 쿠폰 상태 (ISSUED, USED, EXPIRED) |

**Response (200 OK):**
```json
{
  "coupons": [
    {
      "id": 1,
      "couponId": 1,
      "couponName": "신규 회원 10% 할인 쿠폰",
      "discountType": "PERCENT",
      "discountAmount": 10,
      "useMinAmount": 10000,
      "useMaxAmount": 5000,
      "validFrom": "2024-10-30T00:00:00",
      "validUntil": "2024-11-30T23:59:59",
      "status": "ISSUED",
      "issuedAt": "2024-10-30T00:00:00",
      "usedAt": null
    },
    {
      "id": 2,
      "couponId": 2,
      "couponName": "5,000원 할인 쿠폰",
      "discountType": "AMOUNT",
      "discountAmount": 5000,
      "useMinAmount": 30000,
      "useMaxAmount": 5000,
      "validFrom": "2024-10-01T00:00:00",
      "validUntil": "2024-10-31T23:59:59",
      "status": "USED",
      "issuedAt": "2024-10-15T00:00:00",
      "usedAt": "2024-10-20T00:00:00"
    }
  ]
}
```

---

### 4-3. 쿠폰 유효성 검증
주문 금액에 대해 쿠폰 사용 가능 여부를 확인합니다.

**Endpoint:** `POST /api/coupons/{couponHistoryId}/validate`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| couponHistoryId | Long | Yes | 쿠폰 발급 내역 ID |

**Request Body:**
```json
{
  "orderAmount": 50000
}
```

**Response (200 OK):**
```json
{
  "valid": true,
  "discountAmount": 5000,
  "finalAmount": 45000,
  "message": "쿠폰을 사용할 수 있습니다."
}
```

**Error Response (400 Bad Request):**
```json
{
  "valid": false,
  "message": "최소 주문 금액(10,000원)을 충족하지 못했습니다."
}
```

---

## 5. 주문 API

### 5-1. 주문 생성
장바구니의 상품들로 주문을 생성합니다. 재고 확인 및 차감, 쿠폰 적용, 포인트 차감이 트랜잭션으로 처리됩니다.

**Endpoint:** `POST /api/orders`

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "cartItemIds": [1, 2, 3],
  "couponHistoryId": 1,
  "usePoint": 5000
}
```

**Request Body Parameters:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| cartItemIds | Array<Long> | Yes | 주문할 장바구니 아이템 ID 목록 |
| couponHistoryId | Long | No | 사용할 쿠폰 발급 내역 ID |
| usePoint | Integer | No | 사용할 포인트 금액 |

**Response (201 Created):**
```json
{
  "orderId": 1,
  "userId": 1,
  "status": "PENDING",
  "items": [
    {
      "productId": 1,
      "productName": "상품명",
      "optionId": 1,
      "optionType": "색상:블랙",
      "quantity": 2,
      "unitPrice": 10000,
      "totalPrice": 20000
    }
  ],
  "totalAmount": 50000,
  "discountAmount": 5000,
  "pointDiscount": 5000,
  "finalAmount": 40000,
  "orderedAt": "2024-10-30T00:00:00"
}
```

**Error Response (400 Bad Request - 재고 부족):**
```json
{
  "error": "INSUFFICIENT_STOCK",
  "message": "상품 '상품명'의 재고가 부족합니다.",
  "productId": 1,
  "availableStock": 1
}
```

**Error Response (400 Bad Request - 포인트 부족):**
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "포인트 잔액이 부족합니다.",
  "currentBalance": 3000,
  "requiredAmount": 40000
}
```

**Error Response (400 Bad Request - 쿠폰 무효):**
```json
{
  "error": "INVALID_COUPON",
  "message": "쿠폰을 사용할 수 없습니다.",
  "reason": "이미 사용된 쿠폰입니다."
}
```

---

### 5-2. 주문 목록 조회
사용자의 주문 목록을 조회합니다.

**Endpoint:** `GET /api/orders`

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Integer | No | 페이지 번호 (default: 0) |
| size | Integer | No | 페이지 크기 (default: 20) |
| status | String | No | 주문 상태 (PENDING, PAID, CANCELLED) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "orderId": 1,
      "status": "PAID",
      "totalAmount": 50000,
      "finalAmount": 40000,
      "itemCount": 3,
      "orderedAt": "2024-10-30T00:00:00"
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "number": 0
}
```

---

### 5-3. 주문 상세 조회
특정 주문의 상세 정보를 조회합니다.

**Endpoint:** `GET /api/orders/{orderId}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orderId | Long | Yes | 주문 ID |

**Response (200 OK):**
```json
{
  "orderId": 1,
  "userId": 1,
  "status": "PAID",
  "items": [
    {
      "productId": 1,
      "productName": "상품명",
      "optionId": 1,
      "optionType": "색상:블랙",
      "quantity": 2,
      "unitPrice": 10000,
      "totalPrice": 20000
    }
  ],
  "totalAmount": 50000,
  "discountAmount": 5000,
  "pointDiscount": 5000,
  "finalAmount": 40000,
  "coupon": {
    "couponName": "신규 회원 10% 할인 쿠폰",
    "discountAmount": 5000
  },
  "payment": {
    "paymentId": 1,
    "paymentAmount": 40000,
    "paidAt": "2024-10-30T00:00:10"
  },
  "orderedAt": "2024-10-30T00:00:00"
}
```

**Error Response (404 Not Found):**
```json
{
  "error": "NOT_FOUND",
  "message": "주문을 찾을 수 없습니다."
}
```

---

### 5-4. 주문 취소
주문을 취소합니다. 재고, 포인트, 쿠폰이 복구됩니다.

**Endpoint:** `POST /api/orders/{orderId}/cancel`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orderId | Long | Yes | 주문 ID |

**Request Body:**
```json
{
  "reason": "단순 변심"
}
```

**Response (200 OK):**
```json
{
  "orderId": 1,
  "status": "CANCELLED",
  "refundAmount": 40000,
  "refundPoint": 40000,
  "couponRestored": true,
  "cancelledAt": "2024-10-30T00:00:00"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "INVALID_STATUS",
  "message": "이미 결제 완료된 주문은 취소할 수 없습니다."
}
```

---

## 6. 결제 API

### 6-1. 결제 처리
주문에 대한 결제를 처리합니다. 포인트로 결제되며, 성공 시 외부 시스템에 주문 데이터를 비동기로 전송합니다.

**Endpoint:** `POST /api/payments`

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "orderId": 1,
  "paymentMethod": "POINT"
}
```

**Response (200 OK):**
```json
{
  "paymentId": 1,
  "orderId": 1,
  "userId": 1,
  "paymentAmount": 40000,
  "paymentMethod": "POINT",
  "status": "SUCCESS",
  "paidAt": "2024-10-30T00:00:00",
  "earnedPoint": 400
}
```

**Error Response (400 Bad Request - 포인트 부족):**
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "포인트 잔액이 부족합니다.",
  "currentBalance": 30000,
  "requiredAmount": 40000
}
```

**Error Response (400 Bad Request - 이미 결제됨):**
```json
{
  "error": "ALREADY_PAID",
  "message": "이미 결제된 주문입니다."
}
```

---

### 6-2. 결제 조회
특정 주문의 결제 정보를 조회합니다.

**Endpoint:** `GET /api/payments/{orderId}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orderId | Long | Yes | 주문 ID |

**Response (200 OK):**
```json
{
  "paymentId": 1,
  "orderId": 1,
  "userId": 1,
  "paymentAmount": 40000,
  "paymentMethod": "POINT",
  "status": "SUCCESS",
  "paidAt": "2024-10-30T00:00:00"
}
```

**Error Response (404 Not Found):**
```json
{
  "error": "NOT_FOUND",
  "message": "결제 정보를 찾을 수 없습니다."
}
```

---

## 공통 응답 형식

### 성공 응답
```json
{
  "data": { ... },
  "message": "성공 메시지",
  "timestamp": "2024-10-30T00:00:00"
}
```

### 에러 응답
```json
{
  "error": "ERROR_CODE",
  "message": "에러 메시지",
  "details": [ ... ],
  "timestamp": "2024-10-30T00:00:00"
}
```

### HTTP 상태 코드

| Code | Description |
|------|-------------|
| 200 | OK - 요청 성공 |
| 201 | Created - 리소스 생성 성공 |
| 204 | No Content - 성공 (응답 본문 없음) |
| 400 | Bad Request - 잘못된 요청 |
| 401 | Unauthorized - 인증 필요 |
| 403 | Forbidden - 권한 없음 |
| 404 | Not Found - 리소스 없음 |
| 409 | Conflict - 리소스 충돌 |
| 429 | Too Many Requests - 요청 제한 초과 |
| 500 | Internal Server Error - 서버 오류 |

---

## 에러 코드 목록

| Error Code | Description |
|------------|-------------|
| OUT_OF_STOCK | 재고 부족 |
| INSUFFICIENT_BALANCE | 잔액 부족 |
| INVALID_COUPON | 유효하지 않은 쿠폰 |
| ALREADY_ISSUED | 이미 발급된 쿠폰 |
| ALREADY_PAID | 이미 결제된 주문 |
| INVALID_STATUS | 유효하지 않은 상태 |
| NOT_FOUND | 리소스를 찾을 수 없음 |
| INVALID_AMOUNT | 유효하지 않은 금액 |
| TOO_MANY_REQUESTS | 요청 제한 초과 (Rate Limiting) |
| VALIDATION_ERROR | 입력 검증 오류 |

---

## 주요 비즈니스 규칙

### 1. 재고 관리
- 장바구니 추가 시 재고 확인
- 주문 생성 시 재고 차감 (원자적 처리)
- 주문 취소 시 재고 복구

### 2. 포인트 관리
- 최소 충전 금액: 1,000원
- 포인트로만 결제 가능
- 결제 시 포인트 차감
- 결제 완료 시 포인트 적립 (결제 금액의 1%)

### 3. 쿠폰 관리
- 선착순 발급 (동시성 제어)
- 1인 1회 발급 제한
- 유효기간 확인
- 최소 주문 금액 확인
- 최대 할인 금액 제한

### 4. 주문/결제 프로세스
1. 장바구니 → 주문 생성
2. 재고 확인 및 차감
3. 쿠폰 적용 (선택)
4. 포인트 결제
5. 주문 상태 → PAID
6. 외부 시스템에 데이터 전송 (비동기)
7. 포인트 적립

### 5. 외부 데이터 연동
- 주문 완료 후 비동기로 외부 시스템에 전송
- 전송 실패 시 최대 3회 재시도
- 외부 전송 실패해도 주문은 정상 처리 유지
- 전송 내역은 EXTERNAL_SYNC_LOG에 기록

---

**작성일:** 2024-10-30
**최종 수정일:** 2025-11-03
**버전:** 1.1

**변경 이력:**
- v1.1 (2025-11-03): data-models.md v1.2 반영 확인
  - 인기 상품 조회 API 명세 유지
  - 포인트, 주문, 결제 관련 필드 정의 확인
- v1.0 (2024-10-30): 초기 작성
