# 캐시 전략 분석 보고서

## 1. 개요
이커머스 시스템에서 성능 향상과 DB 부하 감소를 위해 캐시가 필요한 영역을 분석하고 적절한 캐싱 전략을 제안합니다.

---

## 2. 캐시 적용 대상

### 2.1 상품 목록 조회 (GetProductsUseCase) ⭐️⭐️⭐️⭐️
**현재 구현:**
```java
public Page<ProductEntity> execute(Pageable pageable) {
    return productRepository.findAll(pageable);
}
```

**캐시 필요 이유:**
- 조회 빈도: 높음 (메인 페이지, 카테고리별 목록 조회)
- 변경 빈도: 낮음
- DB 부하: 중간 (페이징 쿼리, COUNT 쿼리)

**캐싱 전략:**
- **캐시 키:** `products:page:{page}:size:{size}:sort:{sort}`
- **TTL:** 10분 ~ 30분
- **캐시 타입:** Redis String (Page 객체 직렬화)
- **무효화 시점:**
  - 상품 추가/삭제 시 전체 목록 캐시 삭제
  - 상품 수정 시 해당 상품이 포함된 페이지만 삭제
- **최적화:**
  - 첫 페이지(page=0)는 TTL 짧게 설정 (5분)
  - 자주 조회되는 페이지만 선택적 캐싱

**예상 효과:**
- 메인 페이지 로딩 속도 50% 개선
- DB 부하 70% 감소

---

### 2.2 인기 상품 조회 (GetPopularProductsUseCase) ⭐️⭐️⭐️⭐️⭐️
**현재 구현:**
```java
public List<ProductStatisticsEntity> execute(int limit) {
    List<ProductStatisticsEntity> allStatistics = productStatisticsRepository.findAll();
    return allStatistics.stream()
        .sorted(Comparator.comparingLong(ProductStatisticsEntity::getSalesCount).reversed())
        .limit(limit)
        .toList();
}
```

**캐시 필요 이유:**
- 조회 빈도: 매우 높음 (메인 페이지, 추천 영역)
- 계산 비용: 높음 (전날 주문 데이터 집계)
- 변경 빈도: 하루 1회 (매일 자정 갱신)

**캐싱 전략:**
- **캐시 키:** `daily:popular:products`
- **갱신 주기:** 매일 자정 1분 (배치 스케줄러)
- **캐시 타입:** Redis Sorted Set (전날 판매량을 score로 저장)
  ```
  ZADD daily:popular:products {salesCount} {productId}
  ZREVRANGE daily:popular:products 0 {limit-1} WITHSCORES
  ```
- **집계 방식:**
  - 매일 자정 1분에 전날(어제) ORDER_ITEMS 데이터 집계
  - 상품별 판매량(quantity) 합계 계산
  - Redis Sorted Set에 저장
- **배치 스케줄러:** `DailyPopularProductScheduler` (@Scheduled, cron = "0 1 0 * * *")

**예상 효과:**
- 일간 인기상품 기준 명확화 (전날 판매량)
- 실시간 집계 부하 제거 → DB 부하 95% 이상 감소
- 응답 시간 100ms → 2ms 이하로 개선

---

## 3. 구현 상태

### 3.1 상품 목록 조회 캐싱
- ✅ 구현 완료 (ProductCacheService)
- Cache-Aside 패턴 적용
- TTL: 10분

### 3.2 인기 상품 조회 캐싱
- ✅ 구현 완료 (Redis Sorted Set + 배치 스케줄러)
- 매일 자정 1분에 전날 판매 데이터 집계
- 일간 판매량 기준 자동 정렬
- 배치 스케줄러: `DailyPopularProductScheduler`

---

## 4. 예상 성능 개선

| 대상 | DB 부하 감소 | 응답 시간 개선 |
|-----|-------------|--------------|
| 상품 목록 조회 | 70% 이상 | 50% 이상 |
| 인기 상품 조회 | 95% 이상 | 90% 이상 |