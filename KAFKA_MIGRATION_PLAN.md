# 카프카 도입 계획서

## 1. 현재 시스템 구조

### 1.1 이벤트 처리 방식
- **Spring ApplicationEventPublisher** 기반 이벤트 발행
- **@TransactionalEventListener** (AFTER_COMMIT)로 트랜잭션 후 비동기 처리
- **@Async** 비동기 실행
- **@Retryable** 재시도 로직 (최대 3회, 2초 간격)
- **FailedEventEntity**를 통한 실패 이벤트 저장 및 재처리

### 1.2 현재 이벤트 종류
- **OrderCreatedEvent**: 주문 생성 시 발행
- **OrderCancelledEvent**: 주문 취소 시 발행
- 이벤트 리스너에서 판매 랭킹 업데이트 처리

### 1.3 한계점
- 단일 애플리케이션 내부 이벤트 (마이크로서비스 확장 불가)
- 이벤트 순서 보장 어려움
- 메시지 영속성 부족 (애플리케이션 재시작 시 유실 가능)
- 처리량 확장성 제한
- 여러 컨슈머에게 동일 이벤트 브로드캐스트 불가

---

## 2. 카프카 도입 목적

### 2.1 메시지 영속성 및 신뢰성
- 디스크 기반 메시지 저장으로 데이터 유실 방지
- 컨슈머 장애 시 재처리 가능

### 2.2 확장성
- 파티션 기반 병렬 처리
- 컨슈머 그룹으로 수평 확장 가능

### 2.3 마이크로서비스 아키텍처 준비
- 도메인 간 느슨한 결합
- 독립적인 서비스 배포 및 확장

### 2.4 이벤트 소싱 및 감사
- 모든 도메인 이벤트 추적 가능
- 시스템 상태 재구성 가능

---

## 3. 카프카 토픽 설계

### 3.1 주문(Order) 도메인
```
토픽명: order-events
파티션: 3 (주문량에 따라 조정 가능)
키: orderId (동일 주문은 동일 파티션으로 순서 보장)
Replication Factor: 2 (운영 환경)

이벤트 타입:
- ORDER_CREATED: 주문 생성
- ORDER_COMPLETED: 주문 완료
- ORDER_CANCELLED: 주문 취소

메시지 스키마:
{
  "eventType": "ORDER_CREATED",
  "orderId": 123,
  "userId": 456,
  "orderItems": [
    {
      "productId": 10,
      "productOptionId": 20,
      "productName": "상품A",
      "optionType": "LARGE",
      "quantity": 2,
      "price": 25000
    }
  ],
  "totalAmount": 50000,
  "discountAmount": 5000,
  "finalAmount": 45000,
  "couponHistoryId": 100,
  "timestamp": "2025-12-16T10:30:00Z"
}
```

### 3.2 재고(Stock) 도메인
```
토픽명: stock-events
파티션: 5 (재고 변경 빈도가 높을 수 있음)
키: productOptionId (동일 상품은 동일 파티션)
Replication Factor: 2 (운영 환경)

이벤트 타입:
- STOCK_DECREASED: 재고 감소
- STOCK_INCREASED: 재고 증가 (복구)
- STOCK_RESERVED: 재고 예약 (미래 확장)
- STOCK_RELEASED: 재고 예약 해제 (미래 확장)

메시지 스키마:
{
  "eventType": "STOCK_DECREASED",
  "productOptionId": 10,
  "productId": 5,
  "optionType": "LARGE",
  "quantity": 2,
  "previousStock": 100,
  "currentStock": 98,
  "orderId": 123,
  "timestamp": "2025-12-16T10:30:01Z"
}
```

### 3.3 쿠폰(Coupon) 도메인
```
토픽명: coupon-events
파티션: 3
키: userId (동일 사용자는 순서 보장)
Replication Factor: 2 (운영 환경)

이벤트 타입:
- COUPON_ISSUED: 쿠폰 발급
- COUPON_USED: 쿠폰 사용
- COUPON_EXPIRED: 쿠폰 만료

메시지 스키마:
{
  "eventType": "COUPON_ISSUED",
  "couponHistoryId": 999,
  "couponId": 10,
  "couponName": "신규가입 쿠폰",
  "userId": 456,
  "discountType": "PERCENT",
  "discountAmount": 10,
  "timestamp": "2025-12-16T10:25:00Z"
}
```

---

## 4. 프로듀서/컨슈머 구조

### 4.1 프로듀서 설정
- **멱등성(Idempotence)**: enable.idempotence=true
- **Acks**: all (모든 리플리카 확인)
- **재시도**: retries=3
- **순서 보장**: max.in.flight.requests.per.connection=5
- **압축**: compression.type=snappy (선택적)

### 4.2 컨슈머 그룹
```
1. sales-ranking-consumer-group
   - order-events 구독
   - 판매 랭킹 업데이트

2. inventory-sync-consumer-group (미래 확장)
   - stock-events 구독
   - 재고 동기화 및 알림

3. coupon-analytics-consumer-group (미래 확장)
   - coupon-events 구독
   - 쿠폰 사용률 분석

4. notification-consumer-group (미래 확장)
   - order-events, coupon-events 구독
   - 사용자 알림 발송

5. analytics-consumer-group (미래 확장)
   - 모든 토픽 구독
   - 데이터 분석 및 리포팅
```

### 4.3 컨슈머 설정
- **수동 커밋**: enable.auto.commit=false
- **오프셋 리셋**: auto.offset.reset=earliest
- **신뢰할 수 있는 패키지**: spring.json.trusted.packages=*
- **최대 폴 레코드**: max.poll.records=100 (조정 가능)

---

## 5. 구현 계획

### 5.1 인프라 구성
- [x] Kafka 의존성 추가 (build.gradle)
- [x] Kafka 설정 파일 작성 (application.yml)
- [ ] Docker Compose에 Kafka, Zookeeper 추가
- [ ] KafkaProducerConfig 클래스 작성
- [ ] KafkaConsumerConfig 클래스 작성
- [ ] KafkaTopicConfig 클래스 작성 (토픽 자동 생성)

### 5.2 공통 인프라 구현
- [ ] KafkaEventPublisher 인터페이스 및 구현체
- [ ] KafkaEvent 기본 추상 클래스 (공통 필드: eventType, timestamp)
- [ ] 토픽명 상수 정의 (KafkaTopics 클래스)
- [ ] 이벤트 타입 Enum (OrderEventType, StockEventType, CouponEventType)

### 5.3 도메인별 이벤트 정의 (Kafka용)

#### 주문 도메인 (Order)
- [ ] OrderKafkaEvent (추상 클래스)
- [ ] OrderCreatedKafkaEvent
- [ ] OrderCompletedKafkaEvent
- [ ] OrderCancelledKafkaEvent
- [ ] OrderItemDto (주문 아이템 정보)

#### 재고 도메인 (Stock)
- [ ] StockKafkaEvent (추상 클래스)
- [ ] StockDecreasedKafkaEvent
- [ ] StockIncreasedKafkaEvent
- [ ] StockReservedKafkaEvent (미래 확장)
- [ ] StockReleasedKafkaEvent (미래 확장)

#### 쿠폰 도메인 (Coupon)
- [ ] CouponKafkaEvent (추상 클래스)
- [ ] CouponIssuedKafkaEvent
- [ ] CouponUsedKafkaEvent
- [ ] CouponExpiredKafkaEvent

### 5.4 프로듀서 구현

#### 주문 서비스
- [ ] OrderKafkaProducer 클래스
- [ ] CreateOrderUseCase에서 ORDER_CREATED 이벤트 발행
- [ ] CompleteOrderUseCase에서 ORDER_COMPLETED 이벤트 발행
- [ ] CancelOrderUseCase에서 ORDER_CANCELLED 이벤트 발행

#### 재고 서비스
- [ ] StockKafkaProducer 클래스
- [ ] DecreaseStockUseCase에서 STOCK_DECREASED 이벤트 발행
- [ ] 재고 복구 시 STOCK_INCREASED 이벤트 발행

#### 쿠폰 서비스
- [ ] CouponKafkaProducer 클래스
- [ ] IssueCouponUseCase에서 COUPON_ISSUED 이벤트 발행
- [ ] 쿠폰 사용 시 COUPON_USED 이벤트 발행

### 5.5 컨슈머 구현
- [ ] OrderEventKafkaConsumer
  - sales-ranking-consumer-group
  - ORDER_CREATED, ORDER_CANCELLED 처리
  - 판매 랭킹 업데이트 로직
- [ ] StockEventKafkaConsumer (미래 확장)
- [ ] CouponEventKafkaConsumer (미래 확장)

### 5.6 에러 처리 및 재시도
- [ ] DLQ(Dead Letter Queue) 토픽 설정
  - order-events-dlq
  - stock-events-dlq
  - coupon-events-dlq
- [ ] @RetryableTopic 어노테이션 활용
- [ ] 재시도 정책 (3회, exponential backoff)
- [ ] FailedEventEntity와 연동 (DLQ 메시지를 DB에도 저장)
- [ ] DLQ 모니터링 및 알람

### 5.7 테스트
- [ ] Kafka Testcontainers 설정
- [ ] 프로듀서 단위 테스트
  - OrderKafkaProducer 테스트
  - StockKafkaProducer 테스트
  - CouponKafkaProducer 테스트
- [ ] 컨슈머 단위 테스트
  - OrderEventKafkaConsumer 테스트
- [ ] 통합 테스트 (End-to-End)
  - 주문 생성 → Kafka 발행 → 컨슈머 처리 → 랭킹 업데이트
- [ ] 실패 시나리오 테스트
  - 컨슈머 예외 발생 시 재시도
  - DLQ 전송 검증

---

## 6. 마이그레이션 전략

### 6.1 단계적 마이그레이션

#### Phase 1: 인프라 준비 및 병행 운영
- Docker Compose에 Kafka 추가
- 공통 인프라 코드 구현
- 기존 ApplicationEventPublisher 유지하면서 Kafka 병행 발행
- 두 시스템의 결과 비교 및 검증

#### Phase 2: 주문 도메인 전환
- OrderKafkaProducer 구현 및 적용
- OrderEventKafkaConsumer 구현
- 기존 OrderEventListener와 병행 운영
- 모니터링 및 안정성 검증

#### Phase 3: 재고/쿠폰 도메인 전환
- StockKafkaProducer, CouponKafkaProducer 구현
- 각 도메인별 컨슈머 구현 (필요 시)
- 병행 운영 및 검증

#### Phase 4: 기존 시스템 제거
- ApplicationEventPublisher 제거
- 기존 EventListener 제거
- Kafka 완전 전환

### 6.2 롤백 계획
- 기존 이벤트 리스너 코드는 주석 처리하여 유지
- Feature Flag로 Kafka 활성화/비활성화 제어
  ```yaml
  ecommerce:
    event:
      kafka:
        enabled: true  # false로 변경 시 기존 시스템으로 롤백
  ```
- 긴급 상황 시 애플리케이션 재배포 없이 설정만 변경하여 롤백

---

## 7. 모니터링 및 운영

### 7.1 모니터링 지표
- **Producer 지표**
  - 메시지 전송 성공/실패율
  - 평균 전송 지연 시간
  - 전송 처리량 (TPS)
- **Consumer 지표**
  - 컨슈머 랙(Consumer Lag)
  - 메시지 처리 지연 시간
  - 처리 성공/실패율
  - DLQ 유입 메시지 수
- **Kafka 브로커 지표**
  - CPU, 메모리, 디스크 사용률
  - 네트워크 I/O
  - 파티션별 메시지 수

### 7.2 알람 설정
- 컨슈머 랙 > 1000 메시지
- DLQ에 메시지 유입 시
- 프로듀서 전송 실패율 > 1%
- 컨슈머 처리 실패율 > 5%
- Kafka 브로커 다운

### 7.3 운영 도구
- **Kafka UI**: 토픽, 메시지, 컨슈머 그룹 모니터링
- **Prometheus + Grafana**: 메트릭 수집 및 대시보드
- **로그 수집**: ELK Stack 또는 Splunk

---

## 8. 기대 효과

1. **시스템 신뢰성 향상**
   - 메시지 유실 방지 (디스크 기반 저장)
   - 컨슈머 장애 시 자동 재처리

2. **확장성**
   - 트래픽 증가 시 파티션 및 컨슈머 수평 확장
   - 도메인별 독립적인 확장 가능

3. **마이크로서비스 아키텍처 준비**
   - 주문, 재고, 쿠폰 서비스 분리 기반 마련
   - 도메인 간 느슨한 결합

4. **이벤트 추적 및 감사**
   - 모든 비즈니스 이벤트 영구 저장
   - 디버깅 및 분석 용이

5. **성능 향상**
   - 비동기 처리로 사용자 응답 시간 단축
   - 배치 처리 가능 (컨슈머 최적화)

---

## 9. 리스크 및 대응 방안

### 9.1 운영 복잡도 증가
- **리스크**: Kafka 클러스터 운영 및 관리 부담
- **대응**:
  - Kafka UI 도입으로 운영 편의성 확보
  - 충분한 모니터링 및 알람 설정
  - 운영 가이드 문서 작성

### 9.2 메시지 순서 보장
- **리스크**: 파티션 간 메시지 순서 보장 불가
- **대응**:
  - 동일 엔티티(orderId, userId 등)는 동일 파티션으로 라우팅
  - 파티션 키 전략 명확히 정의

### 9.3 중복 메시지 처리
- **리스크**: At-Least-Once 방식으로 중복 가능
- **대응**:
  - 컨슈머 로직을 멱등성 있게 설계
  - 이벤트 ID 기반 중복 체크

### 9.4 스키마 변경 관리
- **리스크**: 이벤트 스키마 변경 시 호환성 문제
- **대응**:
  - 하위 호환성 유지 (필드 추가만 허용)
  - 필드 제거/변경 시 버전 관리
  - Schema Registry 도입 검토 (미래)

### 9.5 성능 저하
- **리스크**: 네트워크 I/O로 인한 지연
- **대응**:
  - 비동기 전송으로 블로킹 최소화
  - 배치 전송 설정 (linger.ms, batch.size)
  - 적절한 파티션 수 설정

---

## 10. 구현 상세 가이드

### 10.1 Docker Compose 구성
```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'
```

### 10.2 토픽 생성 전략
- 애플리케이션 시작 시 자동 생성 (KafkaTopicConfig)
- 또는 수동 생성 스크립트 제공

### 10.3 이벤트 발행 패턴
```java
// UseCase에서 비즈니스 로직 실행 후 이벤트 발행
public void createOrder(...) {
    // 1. 비즈니스 로직 (트랜잭션 내)
    OrderEntity order = orderRepository.save(...);

    // 2. Kafka 이벤트 발행 (트랜잭션 후)
    kafkaProducer.publish(
        new OrderCreatedKafkaEvent(order)
    );
}
```

### 10.4 컨슈머 처리 패턴
```java
@KafkaListener(topics = "order-events")
public void consume(OrderKafkaEvent event, Acknowledgment ack) {
    try {
        // 비즈니스 로직 처리
        processEvent(event);

        // 수동 커밋
        ack.acknowledge();
    } catch (Exception e) {
        // 예외 처리 (재시도 또는 DLQ)
        log.error("Failed to process event", e);
        throw e; // @RetryableTopic이 재시도 처리
    }
}
```

---

## 11. 다음 단계

### 즉시 실행
1. Docker Compose 파일 수정 (Kafka, Zookeeper 추가)
2. 공통 인프라 코드 구현
   - KafkaTopics
   - KafkaEventPublisher
   - Config 클래스

### 단기 (1-2주)
3. 주문 도메인 Kafka 적용
   - 이벤트 정의
   - Producer/Consumer 구현
   - 통합 테스트

### 중기 (2-4주)
4. 재고, 쿠폰 도메인 Kafka 적용
5. 병행 운영 및 모니터링
6. 성능 테스트 및 최적화

### 장기 (4주+)
7. 기존 ApplicationEventPublisher 제거
8. 추가 컨슈머 그룹 구현 (알림, 분석 등)
9. 마이크로서비스 분리 검토

---

## 12. 참고 자료

- Spring Kafka 공식 문서
- Apache Kafka 공식 문서
- Kafka 모범 사례 (Best Practices)
- 이벤트 소싱 패턴 가이드