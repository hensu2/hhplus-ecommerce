# 재고(Stock) 도메인 Kafka 마이그레이션 계획서

## 1. 현재 상태 분석

### 재고 관련 UseCase
- **UpdateStockUseCase**: 재고 수정 (SET, INCREASE, DECREASE)
- **DecreaseStockUseCase**: 재고 감소 (분산 락 사용)
- **GetProductStockUseCase**: 재고 조회

### 재고 변경 지점
1. UpdateStockUseCase에서 직접 재고 수정
2. CreateOrderUseCase에서 재고 감소
3. CancelOrderUseCase에서 재고 복구

### 현재 문제점
- 재고 변경 이벤트 없음
- 재고 이력 추적 불가
- 재고 부족 알림 불가
- 타 서비스와의 연동 불가

---

## 2. Kafka 마이그레이션 목표

### 이벤트 발행 목적
1. **재고 이력 관리**: 재고 변경 내역 추적
2. **재고 알림**: 재고 부족 시 알림 발송
3. **데이터 분석**: 재고 회전율, 품절 빈도 등 분석
4. **타 서비스 연동**: 재입고 알림, 물류 시스템 연동

---

## 3. 이벤트 설계

### 3.1 이벤트 타입 (StockEventType)
```java
public enum StockEventType {
    STOCK_INCREASED,  // 재고 증가
    STOCK_DECREASED,  // 재고 감소
    STOCK_UPDATED     // 재고 설정
}
```

### 3.2 기본 이벤트 클래스
```java
@Getter
@NoArgsConstructor
@JsonTypeInfo(...)
@JsonSubTypes(...)
public abstract class StockKafkaEvent {
    protected StockEventType eventType;
    protected Long productOptionId;
    protected Long timestamp;
}
```

### 3.3 재고 변경 이벤트
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockChangedKafkaEvent extends StockKafkaEvent {
    private Long productId;
    private String productName;
    private String optionType;
    private Integer previousStock;
    private Integer currentStock;
    private Integer changeAmount;
    private String changeReason; // "ORDER_CREATED", "ORDER_CANCELLED", "ADMIN_UPDATE"
}
```

---

## 4. 구현 계획

### 4.1 Event 클래스 생성
- [x] `StockEventType.java` - enum
- [x] `StockKafkaEvent.java` - 추상 클래스
- [x] `StockChangedKafkaEvent.java` - 구체 이벤트

### 4.2 Producer 구현
- [x] `StockKafkaProducer.java`
  - 파티션 키: productOptionId (동일 상품 옵션은 순서 보장)
  - 토픽: stock-events

### 4.3 Kafka 설정
- [x] `KafkaTopicConfig.java` 수정
  - stock-events 토픽 추가 (파티션 3, 리플리케이션 1)
  - stock-events-dlq 토픽 추가
- [x] `KafkaTopics.java` 수정
  - STOCK_EVENTS, STOCK_EVENTS_DLQ 상수 추가

### 4.4 Consumer 구현 (선택)
- [ ] `StockEventKafkaConsumer.java` (필요 시)
  - 재고 이력 저장
  - 재고 부족 알림
  - 데이터 분석용 집계

### 4.5 UseCase 수정 (Dual Write Pattern)
- [x] `UpdateStockUseCase.java`
  - 기존 로직 유지
  - Kafka 이벤트 발행 추가
- [x] `DecreaseStockUseCase.java`
  - 기존 로직 유지
  - Kafka 이벤트 발행 추가

### 4.6 테스트 작성
- [x] `StockKafkaProducerTest.java` (4 tests)
  - 재고 증가 이벤트 발행
  - 재고 감소 이벤트 발행
  - 여러 이벤트 순서 보장
- [x] `StockKafkaE2ETest.java` (4 tests)
  - 재고 수정 → Kafka 발행 전체 플로우
  - 재고 감소 → Kafka 발행 전체 플로우

---

## 5. 파티션 및 순서 보장 전략

### 파티션 키 전략
- **키**: `productOptionId`
- **이유**: 동일 상품 옵션의 재고 변경은 순서 보장 필요

### 예시
```
productOptionId=1 → 파티션 0
  - DECREASED (재고 100 → 90)
  - DECREASED (재고 90 → 80)
  - INCREASED (재고 80 → 90)

productOptionId=2 → 파티션 1
  - DECREASED (재고 50 → 40)
```

---

## 6. 재시도 및 DLQ 전략

### 재시도 설정
```java
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 2000, multiplier = 2.0)
)
```

### DLQ 처리
- stock-events-dlq 토픽에 실패 메시지 저장
- FailedEventEntity 테이블에 저장
- 수동 재처리 API 제공

---

## 7. 마이그레이션 단계

### Phase 1: Kafka 인프라 구축
1. Event 클래스 생성
2. Producer 구현
3. 토픽 설정

### Phase 2: Dual Write 적용
1. UpdateStockUseCase 수정
2. DecreaseStockUseCase 수정
3. 기존 로직은 유지하면서 Kafka 이벤트 추가 발행

### Phase 3: Consumer 구현 (선택)
1. 재고 이력 Consumer (필요 시)
2. 재고 알림 Consumer (필요 시)

### Phase 4: 테스트 및 검증
1. 단위 테스트 작성
2. E2E 테스트 작성
3. 성능 테스트

---

## 8. 주의사항

### 트랜잭션 관리
- Kafka 이벤트 발행 실패 시에도 DB 트랜잭션은 정상 커밋
- try-catch로 Kafka 발행 에러 처리

### 멱등성
- Consumer는 멱등하게 설계 (동일 이벤트 중복 처리 방지)

### 모니터링
- Kafka lag 모니터링
- DLQ 메시지 모니터링
- 재고 이벤트 발행 실패율 모니터링

---

## 9. 예상 효과

1. **재고 추적성 향상**: 모든 재고 변경 내역 추적 가능
2. **확장성**: Consumer 추가로 다양한 기능 구현 가능
3. **시스템 간 결합도 감소**: 이벤트 기반 비동기 통신
4. **데이터 분석**: 재고 패턴 분석 가능

---

## 10. 구현 일정

- [x] 계획서 작성
- [ ] Event 클래스 생성
- [ ] Producer 구현
- [ ] UseCase 수정
- [ ] 테스트 작성 및 검증
