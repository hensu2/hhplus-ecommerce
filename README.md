# hhplus-ecommerce

## 동시성 제어 분석

### 1. 개요

본 이커머스 시스템은 다중 사용자 환경에서 발생할 수 있는 동시성 문제를 해결하기 위해 **ConcurrentHashMap**과 **원자적 연산(Atomic Operations)**을 활용한 동시성 제어 메커니즘을 구현했습니다.

### 2. 동시성 문제가 발생할 수 있는 주요 시나리오

#### 2.1 쿠폰 발급 시 재고 차감
- **문제**: 여러 사용자가 동시에 같은 쿠폰을 발급받을 때 재고가 음수가 될 수 있음
- **시나리오**:
  1. 사용자 A와 B가 동시에 마지막 1개 남은 쿠폰을 조회
  2. 둘 다 재고가 1개 있음을 확인
  3. 둘 다 재고 차감을 시도
  4. 결과적으로 재고가 -1이 됨

#### 2.2 상품 재고 차감
- **문제**: 주문 생성 시 여러 사용자가 동시에 같은 상품을 주문하면 재고가 실제보다 많이 차감될 수 있음
- **시나리오**:
  1. 재고 10개인 상품에 대해 5명이 동시에 3개씩 주문
  2. Read-Modify-Write 과정에서 Race Condition 발생
  3. 재고가 올바르게 차감되지 않아 overselling 발생

#### 2.3 상품 조회수 증가
- **문제**: 여러 사용자가 동시에 같은 상품을 조회할 때 조회수가 정확하게 증가하지 않을 수 있음
- **시나리오**:
  1. 10명이 동시에 상품을 조회
  2. 모두 조회수 100을 읽음
  3. 모두 101로 업데이트
  4. 실제로는 110이 되어야 하지만 101만 기록됨

### 3. 동시성 제어 구현 방식

#### 3.1 ConcurrentHashMap 선택 이유

```java
private final ConcurrentHashMap<Long, T> table = new ConcurrentHashMap<>();
```

**장점:**
- Thread-safe한 Map 구현체
- 세그먼트 단위 락(Lock Striping)으로 높은 동시성 성능
- 읽기 작업은 락 없이 수행 가능
- 전체 맵에 대한 락이 아닌 버킷 단위 락으로 성능 최적화

**vs HashMap + synchronized:**
- synchronized는 전체 맵에 대한 락으로 성능 저하
- ConcurrentHashMap은 여러 스레드가 동시에 다른 버킷에 접근 가능

**vs Collections.synchronizedMap:**
- 모든 작업에 대해 전체 맵 락 필요
- ConcurrentHashMap보다 성능이 떨어짐

#### 3.2 compute() 메서드를 통한 원자적 연산

**기본 원리:**
```java
table.compute(key, (k, existing) -> {
    // 이 블록 내부는 해당 key에 대해 원자적으로 실행됨
    // 다른 스레드가 같은 key에 접근 불가
    if (existing == null) {
        return createNew();
    }
    return existing.modify();
});
```

**왜 compute()를 사용하는가?**
```java
// ❌ 잘못된 방법 - Race Condition 발생
T value = map.get(key);
if (value != null) {
    T updated = value.modify();
    map.put(key, updated);  // 다른 스레드가 중간에 수정할 수 있음
}

// ✅ 올바른 방법 - 원자적 연산
map.compute(key, (k, v) -> {
    return v == null ? createNew() : v.modify();
});
```

### 4. 구현된 동시성 제어 메커니즘

#### 4.1 쿠폰 재고 관리 (CouponTable.java)

```java
public CouponEntity decreaseStock(long couponId) {
    CouponEntity result = table.compute(couponId, (id, existing) -> {
        if (existing == null) {
            throw new IllegalStateException("쿠폰을 찾을 수 없습니다.");
        }
        return existing.decreaseStock();  // 재고 검증 후 차감
    });

    if (result == null) {
        throw new IllegalStateException("쿠폰 재고 차감에 실패했습니다.");
    }

    return result;
}
```

**동작 방식:**
1. `compute()` 메서드가 해당 couponId에 대한 락 획득
2. 람다 함수 내부에서 재고 확인 및 차감을 원자적으로 수행
3. `existing.decreaseStock()`에서 재고가 0이면 예외 발생
4. 다른 스레드는 이 작업이 완료될 때까지 대기
5. 락 해제 후 다음 스레드가 접근

**동시성 보장:**
- 두 스레드가 동시에 같은 쿠폰의 마지막 재고를 차감하려 해도
- compute() 내부에서 순차적으로 처리되어 한 스레드만 성공
- 다른 스레드는 재고 부족 예외를 받음

#### 4.2 상품 재고 관리 (ProductOptionTable.java)

```java
public ProductOptionEntity decreaseStock(Long optionId, long quantity) {
    ProductOptionEntity result = table.compute(optionId, (id, existing) -> {
        if (existing == null) {
            throw new IllegalStateException("상품 옵션을 찾을 수 없습니다.");
        }
        return existing.updateStock(StockUpdateType.DECREASE, (int) quantity);
    });

    if (result == null) {
        throw new IllegalStateException("재고 차감에 실패했습니다.");
    }

    return result;
}
```

**ProductOptionEntity.updateStock() 내부:**
```java
public ProductOptionEntity updateStock(StockUpdateType type, int amount) {
    long newStock = switch (type) {
        case DECREASE -> {
            long result = this.stock - amount;
            if (result < 0) {
                throw new InvalidStockUpdateException("재고가 부족합니다. 현재 재고: " + this.stock);
            }
            yield result;
        }
        // ... other cases
    };

    return new ProductOptionEntity(/* updated fields */);
}
```

**동시성 보장:**
- 여러 주문이 동시에 같은 상품 옵션을 주문해도
- compute() 덕분에 순차적으로 재고 차감
- 재고가 부족하면 해당 주문은 실패하고 예외 발생

#### 4.3 상품 조회수 관리 (ProductStatisticsTable.java)

```java
public ProductStatisticsEntity incrementViewCount(long productId) {
    return table.compute(productId, (id, existing) -> {
        if (existing == null) {
            return new ProductStatisticsEntity(id, 1, 0, System.currentTimeMillis());
        }
        return existing.increaseViewCount();
    });
}

public ProductStatisticsEntity incrementSalesCount(long productId, int quantity) {
    return table.compute(productId, (id, existing) -> {
        if (existing == null) {
            return new ProductStatisticsEntity(id, 0, quantity, System.currentTimeMillis());
        }
        return existing.increaseSalesCount(quantity);
    });
}
```

**동작 방식:**
1. 통계가 없으면 새로 생성 (초기값 설정)
2. 있으면 기존 값에 증가
3. compute() 덕분에 여러 스레드가 동시에 증가해도 정확한 카운트 보장

**동시성 보장:**
- 100명이 동시에 상품을 조회해도
- 조회수가 정확히 100 증가
- Lost Update 문제 방지

### 5. 동시성 제어 흐름도

#### 5.1 쿠폰 발급 시나리오

```
Thread A: 쿠폰 1번 발급 요청 (재고 1개)
Thread B: 쿠폰 1번 발급 요청 (재고 1개)

Time ──────────────────────────────────────────>

Thread A: |─ compute() 진입 ─|─ 재고 확인(1) ─|─ 재고 차감(0) ─|─ 완료 ─|

Thread B:         |─ 대기 중... ──────────────────|─ compute() 진입 ─|─ 재고 확인(0) ─|─ 예외 발생 ─|
```

#### 5.2 상품 재고 차감 시나리오

```
Thread A: 상품 옵션 1번, 5개 주문
Thread B: 상품 옵션 1번, 3개 주문
Thread C: 상품 옵션 1번, 4개 주문
(현재 재고: 10개)

Thread A: |─ compute() 진입 ─|─ 재고 10 → 5 ─|─ 완료 ─|
Thread B:         |─ 대기 ──────|─ compute() 진입 ─|─ 재고 5 → 2 ─|─ 완료 ─|
Thread C:                 |─ 대기 ─────────────────|─ compute() 진입 ─|─ 재고 부족 예외 ─|
```

### 6. 성능 고려사항

#### 6.1 ConcurrentHashMap의 성능 특성

**장점:**
- 읽기 작업은 대부분 락 없이 수행
- 버킷 단위 락으로 여러 스레드가 동시에 다른 키에 접근 가능
- 높은 처리량(throughput) 달성

**주의사항:**
- compute() 사용 시 해당 키에 대한 락 획득
- 람다 내부 로직이 길면 다른 스레드가 대기
- 따라서 compute() 내부는 최대한 간결하게 유지

#### 6.2 적용된 최적화

```java
// ✅ 좋은 예 - compute() 내부를 간결하게
public CouponEntity decreaseStock(long couponId) {
    return table.compute(couponId, (id, existing) -> {
        if (existing == null) throw new IllegalStateException("...");
        return existing.decreaseStock();  // 빠른 연산
    });
}

// ❌ 나쁜 예 - compute() 내부에서 무거운 작업
public CouponEntity decreaseStock(long couponId) {
    return table.compute(couponId, (id, existing) -> {
        // DB 조회, 외부 API 호출 등 - 다른 스레드가 오래 대기
        callExternalAPI();
        return existing.decreaseStock();
    });
}
```

### 7. 테스트 전략

#### 7.1 단위 테스트에서의 동시성 검증

현재 구현된 테스트는 Mock을 사용하여 단일 스레드에서 동작을 검증합니다.

```java
@Test
void shouldDecreaseStockAtomically() {
    // given
    when(couponRepository.decreaseStock(couponId)).thenReturn(decreasedCoupon);

    // when
    issueCouponUseCase.execute(userId, couponId);

    // then
    verify(couponRepository).decreaseStock(couponId);
}
```

#### 7.2 통합 테스트에서의 동시성 검증 (권장)

실제 동시성 문제를 검증하려면 다음과 같은 테스트가 필요합니다:

```java
@Test
void shouldHandleConcurrentCouponIssuance() throws InterruptedException {
    // given
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    // when - 10개 스레드가 동시에 쿠폰 발급 시도 (재고는 5개)
    for (int i = 0; i < threadCount; i++) {
        final long userId = i;
        executorService.submit(() -> {
            try {
                issueCouponUseCase.execute(userId, couponId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executorService.shutdown();

    // then
    assertThat(successCount.get()).isEqualTo(5);  // 5개만 성공
    assertThat(failCount.get()).isEqualTo(5);     // 5개는 실패
}
```

### 8. 한계 및 개선 방안

#### 8.1 현재 구현의 한계

1. **In-Memory 저장소 사용**
   - 애플리케이션 재시작 시 데이터 손실
   - 다중 인스턴스 환경에서 동기화 불가

2. **분산 환경 미지원**
   - ConcurrentHashMap은 단일 JVM 내에서만 동작
   - 여러 서버에 걸친 동시성 제어 불가

#### 8.2 실제 프로덕션 환경 개선 방안

**1. 데이터베이스 비관적 락 (Pessimistic Lock)**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Coupon c WHERE c.id = :id")
Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
```

**2. 데이터베이스 낙관적 락 (Optimistic Lock)**
```java
@Entity
public class Coupon {
    @Version
    private Long version;
    // ... other fields
}
```

**3. Redis 분산 락**
```java
@RedisLock(key = "coupon:{#couponId}")
public void decreaseStock(Long couponId) {
    // 락 획득 후 재고 차감
}
```

**4. 메시지 큐를 통한 순차 처리**
```java
// 쿠폰 발급 요청을 큐에 넣고 순차 처리
kafkaTemplate.send("coupon-issuance", request);
```

### 9. 결론

본 프로젝트는 ConcurrentHashMap과 compute() 메서드를 활용하여 단일 인스턴스 환경에서의 동시성 문제를 효과적으로 해결했습니다. 주요 성과는 다음과 같습니다:

1. **재고 관리의 정확성 보장**: 쿠폰 및 상품 재고가 음수가 되거나 overselling 되는 문제 방지
2. **통계 데이터의 정확성**: 조회수 및 판매량이 정확하게 집계
3. **높은 동시성 처리 성능**: Lock Striping을 통한 효율적인 멀티스레드 처리

실제 프로덕션 환경에서는 데이터베이스 락이나 Redis 분산 락을 고려해야 하지만, 본 프로젝트의 구현은 동시성 제어의 핵심 개념과 원리를 잘 보여줍니다.
