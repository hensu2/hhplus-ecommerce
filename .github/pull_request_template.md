## :pushpin: PR 제목
[STEP 15-16] 정현수 - e-commerce

---
### STEP 15 Application Event
- [x] 주문 정보를 원 트랜잭션이 종료된 이후에 전송
  - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 적용
  - 주문 생성/취소 이벤트를 트랜잭션 커밋 후 비동기 처리
  - 파일: `EventListener.java:35-75`

- [x] 주문 정보를 전달하는 부가 로직에 대한 관심사를 메인 서비스에서 분리
  - `OrderEventListener`를 별도 컴포넌트로 분리
  - 판매 랭킹 업데이트 로직을 주문 서비스에서 완전히 분리
  - `@Async`를 통한 비동기 처리로 메인 로직과 독립 실행

### STEP 16 Transaction Diagnosis
- [x] 도메인별로 트랜잭션이 분리되었을 때 발생 가능한 문제 파악
  - **시나리오**: 주문 생성 → 재고 차감 (성공) → 판매 랭킹 업데이트 (실패)
  - **핵심 판단**: 판매 랭킹은 **부가 로직**이므로, 실패해도 핵심 비즈니스(주문)는 영향받지 않아야 함
  - **설계 원칙**: 부가 로직 실패가 핵심 비즈니스를 롤백하면 안 됨
  - **결과**: 주문은 정상 완료, 랭킹 업데이트 실패는 별도 처리 (DLQ → 수동 재처리)

- [x] 트랜잭션이 분리되더라도 데이터 일관성을 보장할 수 있는 분산 트랜잭션 설계
  - **자동 재시도**: `@Retryable(maxAttempts=3, backoff=@Backoff(delay=2000))` - 일시적 장애 대응
  - **DLQ 패턴**: 재시도 최종 실패 시 실패 이벤트를 DB에 저장 (Dead Letter Queue)
  - **스케줄러 자동 재처리**: 5분마다 PENDING 이벤트 자동 재처리 (최대 5회)
  - **실패 추적**: 모든 실패 이력을 DB에 저장하여 완전한 추적 가능성 확보
  - **수동 재처리 API**: 관리자가 실패 이벤트를 조회하고 재처리 가능
  - **보상 트랜잭션**: 주문 취소 시 재고 복구 (Redisson 분산 락 활용)

---

## 📋 구현 상세

### 1. 주문 취소 시 재고 복구
- **파일**: `CancelOrderUseCase.java`
- **로직**: Redisson MultiLock을 사용한 분산 락으로 동시성 제어
- **구현**: 주문 아이템별로 `ProductOptionEntity.updateStock(INCREASE, quantity)` 실행
- **안전성**: 락 획득 실패 시 예외 발생, finally 블록에서 락 해제 보장
- **테스트**: `CancelOrderUseCaseTest.java` - 재고 복구 검증 통과

### 2. Spring Retry 설정
- **의존성**: `spring-retry`, `spring-aspects` 추가 (`build.gradle`)
- **설정**: `RetryConfig.java`에 `@EnableRetry` 적용
- **전략**: AOP 기반 선언적 재시도로 비즈니스 로직과 분리

### 3. 이벤트 리스너 재시도 로직
- **파일**: `OrderEventListener.java`
- **자동 재시도**: 최대 3회, 2초 간격 (지수 백오프 가능)
- **실패 처리**: `@Recover` 메서드에서 `FailedEventEntity` 생성 및 저장
- **JSON 직렬화**: `ObjectMapper`로 이벤트를 JSON 형태로 저장하여 재현 가능
- **핵심**: 주문은 롤백하지 않고, 실패 이벤트만 DB에 저장하여 DLQ 역할 수행

### 4. DLQ 패턴 및 자동 재처리 프로세스

#### 4-1. 이벤트 처리 플로우
```
[정상 플로우]
주문 생성 → 재고 차감 → OrderCreatedEvent 발행
  → 판매 랭킹 업데이트 성공 → 완료

[DLQ 플로우 - 부가 로직 실패]
주문 생성 → 재고 차감 → 주문 완료 ✅
  → OrderCreatedEvent 발행
    → 판매 랭킹 업데이트 실패
      ├─ 1차 재시도 (2초 후, @Retryable)
      ├─ 2차 재시도 (2초 후)
      ├─ 3차 재시도 (2초 후)
      └─ 최종 실패
          └─ FailedEvent DB 저장 (DLQ, PENDING 상태)
              ↓
          [스케줄러 자동 재처리] (5분마다)
              ├─ 1차 자동 재시도
              ├─ 2차 자동 재시도 (5분 후)
              ├─ 3차 자동 재시도 (5분 후)
              ├─ 4차 자동 재시도 (5분 후)
              ├─ 5차 자동 재시도 (5분 후)
              └─ 최대 재시도 초과 → FAILED 상태
                  → 관리자 수동 재처리 필요

[주문 취소 플로우 - 보상 트랜잭션]
주문 취소 요청 → 재고 복구 (분산 락) → 주문 상태 CANCELLED
  → OrderCancelledEvent 발행
    → 판매 랭킹 차감 (재시도 가능)
```

#### 4-2. 스케줄러 자동 재처리
- **파일**: `FailedEventScheduler.java`
- **실행 주기**: 5분마다 (`fixedDelay = 300000ms`)
- **처리 대상**: PENDING 상태의 FailedEvent
- **최대 재시도**: 5회 초과 시 FAILED 상태로 변경
- **로직**:
  1. PENDING 상태 이벤트 조회
  2. retryCount < 5 확인
  3. RetryFailedEventUseCase 호출하여 재처리
  4. 성공 시 SUCCESS, 실패 시 retryCount 증가 후 PENDING 유지
  5. 최대 횟수 초과 시 FAILED로 변경

#### 4-3. 설계 핵심 원칙
- **부가 로직 분리**: 랭킹 업데이트 실패가 주문에 영향 없음
- **Eventually Consistent**: 최종적으로는 일관성 보장 (자동 + 수동 재처리)
- **명확한 책임**: 핵심 비즈니스 vs 부가 로직 명확히 구분
- **자동화**: 수동 개입 최소화, 일시적 장애는 자동 복구

### 5. 실패 이벤트 관리
- **Domain**: `FailedEventEntity`, `EventType`, `FailedEventStatus`
- **Repository**: `FailedEventRepository`, `FailedEventJpaRepository`
- **UseCase**: `GetFailedEventsUseCase`, `RetryFailedEventUseCase`
- **API**:
  - `GET /api/events/failed` - 실패 이벤트 목록 조회
  - `GET /api/events/failed?status=PENDING` - 상태별 조회
  - `GET /api/events/failed/{eventId}` - 실패 이벤트 상세 조회
  - `POST /api/events/failed/{eventId}/retry` - 수동 재처리

---

## 🎯 DLQ + 스케줄러 패턴 적용 효과

### Before (재시도 없음)
```
주문 생성 → 재고 차감 → 판매 랭킹 업데이트 실패
결과: 랭킹 업데이트 누락, 추적 불가 ❌
```

### After (재시도 + DLQ + 스케줄러 자동 재처리)
```
주문 생성 → 재고 차감 → 주문 완료 ✅
→ 판매 랭킹 업데이트 실패
→ 즉시 자동 재시도 3회 (@Retryable)
→ 최종 실패 시 DLQ에 저장 (PENDING)
→ 스케줄러가 5분마다 자동 재처리 (최대 5회)
→ 여전히 실패 시 FAILED 상태로 변경
→ 관리자 수동 재처리 가능 (API)
결과: 주문 정상 처리, 부가 로직은 자동화된 Eventually Consistent 보장 ✅
```

### 재처리 전략 비교
| 단계 | 방식 | 주기 | 목적 |
|------|------|------|------|
| 1차 방어선 | @Retryable | 즉시 (2초 간격) | 일시적 네트워크 장애 대응 |
| 2차 방어선 | Scheduler | 5분 간격 | 일시적 서비스 장애 자동 복구 |
| 3차 방어선 | Manual API | 관리자 요청 시 | 영구적 장애 수동 개입 |

---

## 📊 생성/수정된 주요 파일

### Domain (이벤트)
- ✨ `FailedEventEntity.java` - 실패 이벤트 엔티티 (DLQ)
- ✨ `EventType.java` - 이벤트 타입 enum
- ✨ `FailedEventStatus.java` - 실패 이벤트 상태 enum

### Application (비즈니스 로직)
- 🔧 `CancelOrderUseCase.java` - 재고 복구 로직 추가 (분산 락)
- 🔧 `OrderEventListener.java` - @Retryable, @Recover 추가 (DLQ 저장)
- ✨ `GetFailedEventsUseCase.java` - 실패 이벤트 조회
- ✨ `RetryFailedEventUseCase.java` - 실패 이벤트 재처리
- ✨ `FailedEventScheduler.java` - 5분마다 자동 재처리 스케줄러

### Infrastructure
- `FailedEventRepository.java` - 실패 이벤트 Repository
- `FailedEventJpaRepository.java` - JPA Repository
- `FailedEventRepositoryImpl.java` - Repository 구현체

### Presentation
- ✨ `EventController.java` - 실패 이벤트 관리 API
- `FailedEventResponse.java` - 응답 DTO
- `FailedEventListResponse.java` - 목록 응답 DTO

### Configuration
- 🔧 `RetryConfig.java` - Spring Retry + Scheduler 설정 (@EnableRetry, @EnableScheduling)
- 🔧 `build.gradle` - spring-retry, spring-aspects 의존성 추가

### Test
- 🔧 `CancelOrderUseCaseTest.java` - 재고 복구 테스트
- 🔧 `OrderEventListenerTest.java` - 이벤트 리스너 테스트
- ✨ `RetryFailedEventUseCaseTest.java` - 재처리 테스트

> ✨ 새로 생성 / 🔧 수정

---

## ✅ 테스트 결과
- **전체 테스트**: 통과 ✓
- **주문 관련 테스트**: 모두 통과 ✓
- **이벤트 관련 테스트**: 모두 통과 ✓
- **재고 복구 테스트**: 모두 통과 ✓

---

## 🔍 핵심 코드 설명

### 1. 재시도와 DLQ 처리
```java
// OrderEventListener.java:38-50
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
public void onOrderCreated(OrderCreatedEvent event) {
    for (OrderItemEntity item : event.getOrderItems()) {
        salesRankingService.increaseRanking(...);
    }
}

@Recover
public void recoverOrderCreated(Exception e, OrderCreatedEvent event) {
    // 실패 이벤트 DB 저장 (주문은 롤백하지 않음)
    FailedEventEntity failedEvent = new FailedEventEntity(...);
    failedEventRepository.save(failedEvent);
    log.info("실패 이벤트 저장 완료 - 수동 재처리 가능");
}
```

### 2. 스케줄러 자동 재처리
```java
// FailedEventScheduler.java
@Component
public class FailedEventScheduler {
    private static final int MAX_RETRY_COUNT = 5;

    @Scheduled(initialDelay = 60000, fixedDelay = 300000) // 1분 후 시작, 5분마다 실행
    public void retryFailedEvents() {
        List<FailedEventEntity> pendingEvents =
            failedEventRepository.findByStatus(FailedEventStatus.PENDING);

        for (FailedEventEntity event : pendingEvents) {
            // 최대 재시도 횟수 초과 확인
            if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                event.markAsFailed("최대 재시도 횟수 초과");
                failedEventRepository.save(event);
                continue;
            }

            try {
                retryFailedEventUseCase.execute(event.getId());
                // 성공 시 SUCCESS로 변경
            } catch (Exception e) {
                // 실패 시 retryCount 증가, PENDING 유지
            }
        }
    }
}
```

### 3. 주문 취소 시 재고 복구
```java
// CancelOrderUseCase.java
@Transactional
public OrderEntity execute(Long orderId) {
    // 1. 분산 락 획득
    RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));
    multiLock.tryLock(10, 5, TimeUnit.SECONDS);

    // 2. 재고 복구
    for (OrderItemEntity item : orderItems) {
        ProductOptionEntity option = productOptionRepository.getOrThrow(...);
        ProductOptionEntity updated = option.updateStock(StockUpdateType.INCREASE, quantity);
        productOptionRepository.save(updated);
    }

    // 3. 주문 취소
    OrderEntity cancelled = new OrderEntity(..., OrderStatus.CANCELLED, ...);
    return orderRepository.save(cancelled);
}
```

---

---

## 📝 트랜잭션 설계 한계점 및 개선 방향

### 현재 설계의 한계점
1. **부분적 일관성**: 랭킹 업데이트 실패 시 최종 일관성만 보장 (Eventually Consistent)
2. **영구적 장애**: 5회 재시도 후에도 실패하면 수동 개입 필요
3. **모니터링 필요**: 실패 이벤트 누적 시 알림 시스템 부재
4. **스케줄러 성능**: 대량 실패 시 스케줄러 부하 가능성

### 설계 근거 및 Trade-off
| 항목 | 선택 | 이유 |
|------|------|------|
| **랭킹 실패 시 주문 롤백** | ❌ 하지 않음 | 부가 로직 실패가 핵심 비즈니스를 중단하면 안 됨 |
| **재시도 횟수** | 3회, 2초 간격 | 일시적 네트워크 장애 대응, 과도한 재시도는 시스템 부하 |
| **DLQ 저장** | ✅ 적용 | 실패 추적 및 수동 재처리 가능, 데이터 손실 방지 |
| **분산 트랜잭션** | ❌ 미적용 | 2PC/3PC는 성능 저하 및 복잡도 증가, Eventually Consistent 선택 |

### 개선 방향
- [x] **스케줄러 자동 재처리**: 5분마다 PENDING 상태 이벤트 자동 재처리 ✅ (완료)
- [ ] **알림 시스템 연동**: FAILED 상태 전환 시 Slack/Email 알림
- [ ] **Outbox Pattern**: 이벤트 발행 실패 방지 (트랜잭션 내 이벤트 저장 → 폴링)
- [ ] **Circuit Breaker**: 연속 실패 시 자동 차단 및 빠른 실패
- [ ] **모니터링 대시보드**: 실패 이벤트 추이, 재처리 성공률 시각화
- [ ] **스케줄러 분산 처리**: 대량 실패 시 배치 처리 또는 병렬 처리

---

### **간단 회고**
- **잘한 점**: 핵심 비즈니스와 부가 로직을 명확히 분리, 3단계 재처리 전략 (즉시 재시도 → 스케줄러 자동 → 수동 재처리)으로 수동 개입 최소화, DLQ 패턴으로 실패 추적 가능성 확보
- **어려운 점**: 비동기 이벤트 처리와 트랜잭션 경계 이해, 부가 로직 실패 시 주문 롤백 여부 판단, 스케줄러 실행 주기와 최대 재시도 횟수 균형 설정
- **배운 점**: 분산 시스템에서 완벽한 일관성보다 Eventually Consistent가 현실적, 자동화로 운영 부담 대폭 감소, Trade-off 기반 설계 결정의 중요성
- **다음 시도**: Outbox Pattern 적용, 알림 시스템 연동 (FAILED 상태 알림), Circuit Breaker 패턴 도입, 모니터링 대시보드 구축