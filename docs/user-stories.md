# User Stories

이커머스 시스템의 사용자 스토리

---

## 목차
1. [상품 관리](#epic-1-상품-관리)
2. [장바구니](#epic-2-장바구니)
3. [포인트 관리](#epic-3-포인트-관리)
4. [쿠폰 시스템](#epic-4-쿠폰-시스템)
5. [주문 관리](#epic-5-주문-관리)
6. [결제](#epic-6-결제)
7. [시스템 관리](#epic-7-시스템-관리)

---

## Epic 1: 상품 관리

### US-1.1: 상품 목록 조회
**As a** 고객
**I want** 판매 중인 상품 목록을 페이지별로 조회하고 싶습니다
**So that** 원하는 상품을 찾아볼 수 있습니다

**Acceptance Criteria:**
- [ ] Given 상품이 등록되어 있을 때, When 상품 목록 페이지에 접근하면, Then 페이지당 20개씩 상품이 표시됩니다
- [ ] Given 상품 목록을 조회할 때, When 페이지 번호를 지정하면, Then 해당 페이지의 상품들이 표시됩니다
- [ ] Given 상품 목록이 표시될 때, Then 상품명, 가격, 등록일이 포함됩니다

**Priority:** High
**Estimate:** 3 Story Points

---

### US-1.2: 상품 상세 정보 조회
**As a** 고객
**I want** 특정 상품의 상세 정보를 확인하고 싶습니다
**So that** 상품의 옵션과 재고를 확인하고 구매를 결정할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 상품 ID가 주어졌을 때, When 상품 상세 페이지에 접근하면, Then 상품의 모든 정보가 표시됩니다
- [ ] Given 상품 상세 정보를 조회할 때, Then 모든 옵션과 각 옵션의 재고가 표시됩니다
- [ ] Given 존재하지 않는 상품 ID로 조회하면, Then 404 에러가 반환됩니다
- [ ] Given 재고가 0인 옵션은, Then 품절로 표시됩니다

**Priority:** High
**Estimate:** 3 Story Points

---

### US-1.3: 인기 상품 조회
**As a** 고객
**I want** 최근 인기 있는 상품을 확인하고 싶습니다
**So that** 다른 사람들이 많이 구매하는 상품을 쉽게 찾을 수 있습니다

**Acceptance Criteria:**
- [ ] Given 최근 3일간의 주문 데이터가 있을 때, When 인기 상품을 조회하면, Then Top 5 상품이 판매량 순으로 표시됩니다
- [ ] Given 인기 상품 목록이 표시될 때, Then 각 상품의 판매 건수와 순위가 함께 표시됩니다
- [ ] Given 최근 3일간 주문이 없으면, Then 빈 목록이 반환됩니다

**Priority:** Medium
**Estimate:** 5 Story Points

---

## Epic 2: 장바구니

### US-2.1: 장바구니 조회
**As a** 고객
**I want** 내 장바구니에 담긴 상품을 확인하고 싶습니다
**So that** 주문하기 전에 장바구니 내용을 검토할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 로그인한 상태에서, When 장바구니 페이지에 접근하면, Then 내가 담은 모든 상품이 표시됩니다
- [ ] Given 장바구니 상품이 표시될 때, Then 상품명, 옵션, 수량, 단가, 총 금액, 현재 재고가 포함됩니다
- [ ] Given 장바구니가 비어있으면, Then 빈 장바구니 메시지가 표시됩니다
- [ ] Given 장바구니 하단에, Then 전체 주문 금액이 표시됩니다

**Priority:** High
**Estimate:** 3 Story Points

---

### US-2.2: 장바구니에 상품 추가
**As a** 고객
**I want** 마음에 드는 상품을 장바구니에 담고 싶습니다
**So that** 나중에 한꺼번에 주문할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 상품 상세 페이지에서, When 옵션과 수량을 선택하고 장바구니에 추가하면, Then 장바구니에 상품이 추가됩니다
- [ ] Given 재고가 부족한 상품을, When 장바구니에 추가하려고 하면, Then 재고 부족 에러가 표시됩니다
- [ ] Given 이미 장바구니에 있는 상품을, When 다시 추가하면, Then 기존 수량에 추가됩니다
- [ ] Given 장바구니 추가가 성공하면, Then 장바구니 페이지로 이동하거나 추가 확인 메시지가 표시됩니다

**Priority:** High
**Estimate:** 5 Story Points

---

### US-2.3: 장바구니 수량 변경
**As a** 고객
**I want** 장바구니에 담긴 상품의 수량을 조절하고 싶습니다
**So that** 원하는 만큼 주문할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 장바구니에 상품이 있을 때, When 수량을 변경하면, Then 변경된 수량이 즉시 반영됩니다
- [ ] Given 수량을 변경할 때, When 재고보다 많은 수량을 입력하면, Then 재고 부족 에러가 표시됩니다
- [ ] Given 수량이 변경되면, Then 상품 금액과 전체 금액이 자동으로 재계산됩니다
- [ ] Given 장바구니 아이템이 존재하지 않으면, Then 404 에러가 반환됩니다

**Priority:** High
**Estimate:** 3 Story Points

---

### US-2.4: 장바구니 상품 삭제
**As a** 고객
**I want** 장바구니에서 필요 없는 상품을 삭제하고 싶습니다
**So that** 원하는 상품만 주문할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 장바구니에 상품이 있을 때, When 삭제 버튼을 클릭하면, Then 해당 상품이 장바구니에서 제거됩니다
- [ ] Given 상품이 삭제되면, Then 전체 금액이 자동으로 재계산됩니다
- [ ] Given 삭제 작업이 성공하면, Then 삭제 확인 메시지가 표시됩니다

**Priority:** High
**Estimate:** 2 Story Points

---

## Epic 3: 포인트 관리

### US-3.1: 포인트 잔액 조회
**As a** 고객
**I want** 내 포인트 잔액을 확인하고 싶습니다
**So that** 결제 시 사용할 수 있는 포인트를 알 수 있습니다

**Acceptance Criteria:**
- [ ] Given 로그인한 상태에서, When 포인트 페이지에 접근하면, Then 현재 포인트 잔액이 표시됩니다
- [ ] Given 포인트 정보를 조회할 때, Then 사용자명과 최근 업데이트 일시가 함께 표시됩니다

**Priority:** High
**Estimate:** 2 Story Points

---

### US-3.2: 포인트 충전
**As a** 고객
**I want** 포인트를 충전하고 싶습니다
**So that** 상품을 구매할 수 있는 잔액을 확보할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 포인트 충전 페이지에서, When 충전 금액을 입력하고 충전하면, Then 포인트가 즉시 증가합니다
- [ ] Given 충전 금액이 1,000원 미만이면, When 충전을 시도하면, Then 최소 금액 에러가 표시됩니다
- [ ] Given 충전이 완료되면, Then 충전 후 잔액이 표시됩니다
- [ ] Given 충전이 완료되면, Then 포인트 내역에 충전 기록이 추가됩니다

**Priority:** High
**Estimate:** 5 Story Points

---

### US-3.3: 포인트 사용 이력 조회
**As a** 고객
**I want** 포인트 충전 및 사용 내역을 확인하고 싶습니다
**So that** 내 포인트 사용 패턴을 파악할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 포인트 내역 페이지에서, When 페이지를 로드하면, Then 최근 내역부터 20개씩 표시됩니다
- [ ] Given 포인트 내역이 표시될 때, Then 각 내역의 금액, 타입(충전/사용/환불), 설명, 일시가 포함됩니다
- [ ] Given 사용 내역은, Then 음수 금액으로 표시됩니다
- [ ] Given 내역이 많으면, Then 페이지네이션으로 이전 내역을 조회할 수 있습니다

**Priority:** Medium
**Estimate:** 3 Story Points

---

## Epic 4: 쿠폰 시스템

### US-4.1: 선착순 쿠폰 발급
**As a** 고객
**I want** 한정 수량의 쿠폰을 발급받고 싶습니다
**So that** 할인 혜택을 받아 상품을 저렴하게 구매할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 쿠폰 발급 페이지에서, When 발급 버튼을 클릭하면, Then 쿠폰이 즉시 발급됩니다
- [ ] Given 쿠폰 재고가 없으면, When 발급을 시도하면, Then 쿠폰 소진 에러가 표시됩니다
- [ ] Given 이미 발급받은 쿠폰을, When 다시 발급받으려고 하면, Then 이미 발급됨 에러가 표시됩니다
- [ ] Given 동시에 여러 사용자가 쿠폰을 발급받으려고 할 때, Then 선착순으로 정확한 수량만 발급됩니다
- [ ] Given 쿠폰 발급이 몰리면, When 발급을 시도하면, Then 429 에러가 표시되고 잠시 후 다시 시도하라는 메시지가 표시됩니다

**Priority:** High
**Estimate:** 8 Story Points

---

### US-4.2: 내 쿠폰 조회
**As a** 고객
**I want** 내가 보유한 쿠폰을 확인하고 싶습니다
**So that** 주문 시 사용할 수 있는 쿠폰을 선택할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 내 쿠폰 페이지에서, When 페이지를 로드하면, Then 발급받은 모든 쿠폰이 표시됩니다
- [ ] Given 쿠폰 목록을 조회할 때, When 상태 필터를 적용하면, Then 해당 상태의 쿠폰만 표시됩니다
- [ ] Given 쿠폰 정보가 표시될 때, Then 쿠폰명, 할인 타입, 할인 금액/비율, 최소 사용 금액, 유효기간, 상태가 포함됩니다
- [ ] Given 사용된 쿠폰은, Then 사용 일시가 함께 표시됩니다
- [ ] Given 유효기간이 지난 쿠폰은, Then EXPIRED 상태로 표시됩니다

**Priority:** High
**Estimate:** 3 Story Points

---

### US-4.3: 쿠폰 유효성 검증
**As a** 고객
**I want** 주문하기 전에 쿠폰 사용 가능 여부를 확인하고 싶습니다
**So that** 쿠폰을 적용하여 실제 결제 금액을 미리 확인할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 주문 페이지에서, When 쿠폰을 선택하고 적용하면, Then 할인 금액이 계산되어 표시됩니다
- [ ] Given 쿠폰의 최소 사용 금액을 충족하지 못하면, Then 최소 금액 미달 에러가 표시됩니다
- [ ] Given 쿠폰의 유효기간이 지났으면, Then 유효기간 만료 에러가 표시됩니다
- [ ] Given 이미 사용된 쿠폰이면, Then 사용 불가 에러가 표시됩니다
- [ ] Given 쿠폰 적용이 성공하면, Then 할인 후 최종 결제 금액이 표시됩니다

**Priority:** High
**Estimate:** 5 Story Points

---

## Epic 5: 주문 관리

### US-5.1: 주문 생성
**As a** 고객
**I want** 장바구니의 상품들로 주문을 생성하고 싶습니다
**So that** 선택한 상품들을 구매할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 장바구니에 상품이 있을 때, When 주문하기 버튼을 클릭하면, Then 주문이 생성됩니다
- [ ] Given 주문 생성 시, When 쿠폰을 선택하면, Then 쿠폰 할인이 적용됩니다
- [ ] Given 주문 생성 시, When 재고가 부족하면, Then 재고 부족 에러가 표시되고 주문이 생성되지 않습니다
- [ ] Given 주문 생성이 완료되면, Then 재고가 차감되고 장바구니가 비워집니다
- [ ] Given 주문 생성이 완료되면, Then 주문 상세 페이지로 이동합니다
- [ ] Given 포인트 잔액이 부족하면, Then 잔액 부족 에러가 표시되고 주문이 생성되지 않습니다

**Priority:** High
**Estimate:** 8 Story Points

---

### US-5.2: 주문 목록 조회
**As a** 고객
**I want** 내가 주문한 내역을 확인하고 싶습니다
**So that** 과거 주문을 추적하고 관리할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 주문 내역 페이지에서, When 페이지를 로드하면, Then 최근 주문부터 20개씩 표시됩니다
- [ ] Given 주문 목록이 표시될 때, Then 주문 ID, 상태, 총 금액, 최종 금액, 상품 개수, 주문 일시가 포함됩니다
- [ ] Given 주문 목록을 조회할 때, When 상태 필터를 적용하면, Then 해당 상태의 주문만 표시됩니다
- [ ] Given 주문이 없으면, Then 빈 목록 메시지가 표시됩니다

**Priority:** High
**Estimate:** 3 Story Points

---

### US-5.3: 주문 상세 조회
**As a** 고객
**I want** 특정 주문의 상세 정보를 확인하고 싶습니다
**So that** 주문한 상품, 금액, 결제 정보를 확인할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 주문 목록에서, When 특정 주문을 클릭하면, Then 주문 상세 페이지가 표시됩니다
- [ ] Given 주문 상세 정보가 표시될 때, Then 주문한 모든 상품의 정보가 포함됩니다
- [ ] Given 주문 상세 정보가 표시될 때, Then 총 금액, 할인 금액, 최종 금액이 명확히 구분되어 표시됩니다
- [ ] Given 쿠폰을 사용했으면, Then 쿠폰 정보와 할인 금액이 표시됩니다
- [ ] Given 결제가 완료되었으면, Then 결제 정보와 결제 일시가 표시됩니다

**Priority:** High
**Estimate:** 5 Story Points

---

### US-5.4: 주문 취소
**As a** 고객
**I want** 결제 전 주문을 취소하고 싶습니다
**So that** 잘못된 주문을 철회하고 재고와 포인트를 복구받을 수 있습니다

**Acceptance Criteria:**
- [ ] Given 결제 대기 중인 주문이 있을 때, When 취소 버튼을 클릭하면, Then 주문이 취소됩니다
- [ ] Given 주문이 취소되면, Then 차감된 재고가 복구됩니다
- [ ] Given 주문이 취소되면, Then 사용된 포인트가 환불됩니다
- [ ] Given 쿠폰을 사용했으면, When 주문을 취소하면, Then 쿠폰이 재사용 가능 상태로 복구됩니다
- [ ] Given 이미 결제가 완료된 주문은, When 취소를 시도하면, Then 취소 불가 에러가 표시됩니다
- [ ] Given 취소 사유를 입력하면, Then 취소 내역에 사유가 기록됩니다

**Priority:** High
**Estimate:** 8 Story Points

---

## Epic 6: 결제

### US-6.1: 포인트 결제 처리
**As a** 고객
**I want** 주문한 상품에 대해 포인트로 결제하고 싶습니다
**So that** 상품을 구매하고 배송받을 수 있습니다

**Acceptance Criteria:**
- [ ] Given 주문이 생성된 상태에서, When 결제 버튼을 클릭하면, Then 포인트가 차감되고 결제가 완료됩니다
- [ ] Given 결제가 완료되면, Then 주문 상태가 PAID로 변경됩니다
- [ ] Given 결제가 완료되면, Then 결제 금액의 1%가 포인트로 적립됩니다
- [ ] Given 포인트 잔액이 부족하면, When 결제를 시도하면, Then 잔액 부족 에러가 표시됩니다
- [ ] Given 이미 결제된 주문을, When 다시 결제하려고 하면, Then 이미 결제됨 에러가 표시됩니다
- [ ] Given 결제가 완료되면, Then 주문 데이터가 외부 시스템에 비동기로 전송됩니다

**Priority:** High
**Estimate:** 8 Story Points

---

### US-6.2: 결제 정보 조회
**As a** 고객
**I want** 특정 주문의 결제 정보를 확인하고 싶습니다
**So that** 결제 내역과 결제 방법을 확인할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 결제가 완료된 주문이 있을 때, When 결제 정보를 조회하면, Then 결제 상세 정보가 표시됩니다
- [ ] Given 결제 정보가 표시될 때, Then 결제 ID, 주문 ID, 결제 금액, 결제 방법, 결제 일시가 포함됩니다
- [ ] Given 결제되지 않은 주문의 결제 정보를 조회하면, Then 404 에러가 반환됩니다

**Priority:** Medium
**Estimate:** 2 Story Points

---

## Epic 7: 시스템 관리

### US-7.1: 주문 데이터 외부 전송
**As a** 시스템 관리자
**I want** 주문 완료 데이터가 자동으로 외부 시스템에 전송되길 원합니다
**So that** 외부 물류 시스템과 데이터를 동기화할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 결제가 완료되면, When 주문 데이터가 생성되면, Then 비동기로 외부 API에 전송됩니다
- [ ] Given 외부 전송이 실패하면, Then 최대 3회까지 자동으로 재시도됩니다
- [ ] Given 3회 재시도 후에도 실패하면, Then FAILED_PERMANENT 상태로 기록되고 관리자에게 알림이 발송됩니다
- [ ] Given 모든 전송 시도는, Then EXTERNAL_SYNC_LOG 테이블에 기록됩니다
- [ ] Given 외부 전송이 실패해도, Then 주문과 결제는 정상적으로 완료됩니다

**Priority:** High
**Estimate:** 8 Story Points

---

### US-7.2: 동시성 제어
**As a** 시스템 관리자
**I want** 선착순 쿠폰 발급 시 동시성이 제어되길 원합니다
**So that** 정확한 수량만큼의 쿠폰이 발급되도록 보장할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 100개의 쿠폰이 있고 1000명이 동시에 발급을 시도할 때, Then 정확히 100명만 쿠폰을 발급받습니다
- [ ] Given 쿠폰 발급 시, Then Redis 분산 락을 사용하여 동시성을 제어합니다
- [ ] Given 락을 획득하지 못한 요청은, Then 429 에러를 반환합니다
- [ ] Given 쿠폰 재고 확인과 차감은, Then 데이터베이스 트랜잭션 내에서 원자적으로 처리됩니다

**Priority:** High
**Estimate:** 13 Story Points

---

### US-7.3: 재고 관리
**As a** 시스템 관리자
**I want** 주문 시 재고가 정확하게 차감되고 관리되길 원합니다
**So that** 재고 부족으로 인한 주문 오류를 방지할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 주문이 생성될 때, Then 재고가 원자적으로 차감됩니다
- [ ] Given 재고가 부족하면, Then 주문 생성이 실패하고 트랜잭션이 롤백됩니다
- [ ] Given 주문이 취소되면, Then 재고가 복구됩니다
- [ ] Given 장바구니 추가 시, Then 실시간으로 재고를 확인합니다
- [ ] Given 재고 차감과 주문 생성은, Then 하나의 트랜잭션으로 처리됩니다

**Priority:** High
**Estimate:** 5 Story Points

---

### US-7.4: 트랜잭션 관리
**As a** 시스템 관리자
**I want** 주문/결제 프로세스가 트랜잭션으로 관리되길 원합니다
**So that** 데이터 무결성을 보장하고 부분 실패를 방지할 수 있습니다

**Acceptance Criteria:**
- [ ] Given 주문 생성 중 에러가 발생하면, Then 모든 변경사항이 롤백됩니다
- [ ] Given 결제 처리 중 에러가 발생하면, Then 포인트 차감, 결제 정보 저장, 주문 상태 변경이 모두 롤백됩니다
- [ ] Given 포인트 충전 중 에러가 발생하면, Then 포인트 증가와 이력 기록이 모두 롤백됩니다
- [ ] Given 쿠폰 발급 중 에러가 발생하면, Then 쿠폰 재고 차감과 발급 기록이 모두 롤백됩니다

**Priority:** High
**Estimate:** 3 Story Points

---

## 우선순위 요약

### Must Have (P0 - High Priority)
- 상품 목록/상세 조회
- 장바구니 전체 기능
- 포인트 조회/충전
- 쿠폰 발급/조회/검증
- 주문 생성/조회/취소
- 결제 처리
- 외부 데이터 전송
- 동시성 제어
- 재고 관리
- 트랜잭션 관리

### Should Have (P1 - Medium Priority)
- 인기 상품 조회
- 포인트 이력 조회
- 결제 정보 조회

### Could Have (P2 - Low Priority)
- (현재 없음)

---

## Story Points 총계
- **Total:** 115 Story Points
- **High Priority:** 106 Story Points
- **Medium Priority:** 9 Story Points

---

**작성일:** 2024-10-30
**버전:** 1.0
**작성 기준:** sequence-diagrams.md v2.0
