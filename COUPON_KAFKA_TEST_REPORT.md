# 쿠폰 Kafka 마이그레이션 테스트 보고서

## 📋 테스트 개요

쿠폰 도메인의 Kafka 마이그레이션이 완료되었으며, 이에 대한 포괄적인 테스트 코드를 작성하였습니다.

### 테스트 범위
- **단위 테스트**: CouponKafkaProducer
- **통합 테스트**: CouponSyncScheduler + Kafka
- **E2E 테스트**: 쿠폰 발급 전체 플로우 (발급 요청 → Redis → DB 동기화 → Kafka 이벤트 발행)

---

## 🧪 작성된 테스트 목록

### 1. CouponKafkaProducer 단위 테스트
**파일**: `src/test/java/com/hhplus/ecommerce/infrastructure/kafka/producer/CouponKafkaProducerTest.java`

#### 테스트 케이스 (총 4개)
1. **PERCENT 타입 쿠폰 발급 이벤트가 정상적으로 발행된다**
   - PERCENT 타입 쿠폰의 이벤트 발행 검증
   - 파티션 키(userId) 검증
   - 이벤트 페이로드 검증 (couponId, couponName, discountType, discountAmount)

2. **AMOUNT 타입 쿠폰 발급 이벤트가 정상적으로 발행된다**
   - AMOUNT 타입 쿠폰의 이벤트 발행 검증
   - 파티션 키(userId) 검증
   - 이벤트 페이로드 검증

3. **PERCENT 타입 - 여러 쿠폰 발급 이벤트가 순서대로 발행된다**
   - 동일 사용자의 여러 이벤트 순서 보장 검증
   - 3개의 PERCENT 쿠폰 이벤트가 동일 파티션에 순서대로 발행

4. **AMOUNT 타입 - 여러 쿠폰 발급 이벤트가 순서대로 발행된다**
   - 동일 사용자의 여러 이벤트 순서 보장 검증
   - 3개의 AMOUNT 쿠폰 이벤트가 동일 파티션에 순서대로 발행

**테스트 환경**
- EmbeddedKafka (port: 9093)
- 파티션: 1
- 토픽: `coupon-events`

---

### 2. CouponSyncScheduler Kafka 통합 테스트
**파일**: `src/test/java/com/hhplus/ecommerce/scheduler/CouponSyncSchedulerKafkaTest.java`

#### 테스트 케이스 (총 4개)
1. **PERCENT 타입 - DB 동기화 시 Kafka 이벤트가 발행된다**
   - Redis pending 데이터 → DB 동기화 검증
   - DB 저장 후 Kafka 이벤트 발행 검증
   - PERCENT 타입 이벤트 페이로드 검증

2. **AMOUNT 타입 - DB 동기화 시 Kafka 이벤트가 발행된다**
   - Redis pending 데이터 → DB 동기화 검증
   - DB 저장 후 Kafka 이벤트 발행 검증
   - AMOUNT 타입 이벤트 페이로드 검증

3. **PERCENT 타입 - 여러 쿠폰 발급이 DB 동기화 시 모두 Kafka 이벤트로 발행**
   - 3명의 사용자 pending 데이터 일괄 동기화
   - 3개의 Kafka 이벤트가 모두 발행되는지 검증
   - 배치 처리 검증

4. **AMOUNT 타입 - 여러 쿠폰 발급이 DB 동기화 시 모두 Kafka 이벤트로 발행**
   - 3명의 사용자 pending 데이터 일괄 동기화
   - 3개의 Kafka 이벤트가 모두 발행되는지 검증
   - 배치 처리 검증

**테스트 환경**
- EmbeddedKafka (port: 9094)
- Redis 연동
- 트랜잭션 롤백 (@Transactional)

---

### 3. 쿠폰 발급 E2E 테스트
**파일**: `src/test/java/com/hhplus/ecommerce/integration/CouponKafkaE2ETest.java`

#### 테스트 케이스 (총 4개)
1. **PERCENT 타입 쿠폰 발급 전체 플로우**
   - Step 1: 쿠폰 발급 요청 (IssueCouponUseCase)
   - Step 2: Redis 큐 → pending 상태로 이동 (Worker 시뮬레이션)
   - Step 3: DB 동기화 (CouponSyncScheduler)
   - Step 4: Kafka 이벤트 발행 검증

2. **AMOUNT 타입 쿠폰 발급 전체 플로우**
   - PERCENT와 동일한 플로우
   - AMOUNT 타입 쿠폰에 대한 검증

3. **PERCENT 타입 - 여러 사용자 동시 쿠폰 발급 시 모두 Kafka 이벤트로 발행**
   - 3명의 사용자가 동시에 쿠폰 발급
   - 모든 발급 요청이 Kafka 이벤트로 발행되는지 검증
   - 동시성 처리 검증

4. **AMOUNT 타입 - 여러 사용자 동시 쿠폰 발급 시 모두 Kafka 이벤트로 발행**
   - 3명의 사용자가 동시에 쿠폰 발급
   - 모든 발급 요청이 Kafka 이벤트로 발행되는지 검증
   - 동시성 처리 검증

**테스트 환경**
- EmbeddedKafka (port: 9095)
- Redis 연동
- 실제 UseCase, Scheduler 사용

---

## 🎯 테스트 커버리지

### 검증 항목
✅ **Kafka 이벤트 발행**
- Producer를 통한 이벤트 발행
- 파티션 키(userId) 검증
- 이벤트 페이로드 검증

✅ **쿠폰 타입별 테스트**
- PERCENT 타입 쿠폰 (퍼센트 할인)
- AMOUNT 타입 쿠폰 (정액 할인)

✅ **이벤트 순서 보장**
- 동일 userId의 이벤트는 동일 파티션에 발행
- 순서 보장 검증

✅ **배치 처리**
- 여러 쿠폰 발급 동시 처리
- 모든 이벤트가 누락 없이 발행되는지 검증

✅ **E2E 플로우**
- 발급 요청 → Redis → DB 동기화 → Kafka 이벤트 발행
- 전체 플로우 통합 테스트

---

## 📊 테스트 실행 결과

### 테스트 파일
- `CouponKafkaProducerTest.java`: 4개 테스트
- `CouponSyncSchedulerKafkaTest.java`: 4개 테스트
- `CouponKafkaE2ETest.java`: 4개 테스트

**총 12개 테스트 케이스**

### 테스트 실행
```bash
./gradlew test --tests "*CouponKafka*"
```

### 보고서 위치
- HTML 보고서: `build/reports/tests/test/index.html`
- JaCoCo 커버리지: `build/reports/jacoco/index.html`

---

## 🔧 테스트 환경 설정

### Gradle 의존성
```gradle
testImplementation 'org.springframework.kafka:spring-kafka-test'
testImplementation 'org.testcontainers:kafka:1.19.3'
testImplementation 'org.awaitility:awaitility:4.2.0'
```

### EmbeddedKafka 설정
- **CouponKafkaProducerTest**: localhost:9093
- **CouponSyncSchedulerKafkaTest**: localhost:9094
- **CouponKafkaE2ETest**: localhost:9095

각 테스트 클래스는 독립적인 포트를 사용하여 충돌 방지

---

## ✅ 검증된 기능

### 1. 이벤트 발행
- [x] CouponKafkaProducer를 통한 이벤트 발행
- [x] KafkaTemplate 정상 동작
- [x] JSON 직렬화/역직렬화

### 2. 파티션 전략
- [x] userId를 파티션 키로 사용
- [x] 동일 사용자의 이벤트는 동일 파티션에 발행
- [x] 순서 보장

### 3. 이벤트 타입별 검증
- [x] PERCENT 타입 쿠폰 이벤트
- [x] AMOUNT 타입 쿠폰 이벤트
- [x] 각 타입의 discountAmount 검증

### 4. 통합 플로우
- [x] IssueCouponUseCase → Redis 큐
- [x] Worker → Redis pending
- [x] CouponSyncScheduler → DB 저장
- [x] CouponKafkaProducer → Kafka 이벤트 발행

### 5. 동시성 처리
- [x] 여러 사용자 동시 발급
- [x] 배치 처리 검증
- [x] 이벤트 누락 없음 검증

---

## 🚀 실행 방법

### 1. Docker로 Kafka 실행
```bash
docker-compose up -d
```

### 2. 테스트 실행
```bash
# 모든 Kafka 관련 테스트 실행
./gradlew test --tests "*CouponKafka*"

# 특정 테스트만 실행
./gradlew test --tests "CouponKafkaProducerTest"
./gradlew test --tests "CouponSyncSchedulerKafkaTest"
./gradlew test --tests "CouponKafkaE2ETest"
```

### 3. 보고서 확인
```bash
# 테스트 보고서
open build/reports/tests/test/index.html

# 커버리지 보고서
open build/reports/jacoco/index.html
```

---

## 📝 테스트 시나리오 요약

### Producer 단위 테스트
1. PERCENT 쿠폰 이벤트 발행 → Consumer 수신 확인
2. AMOUNT 쿠폰 이벤트 발행 → Consumer 수신 확인
3. PERCENT 쿠폰 3개 연속 발행 → 순서 보장 확인
4. AMOUNT 쿠폰 3개 연속 발행 → 순서 보장 확인

### Scheduler 통합 테스트
1. PERCENT 쿠폰 DB 동기화 → Kafka 이벤트 발행 확인
2. AMOUNT 쿠폰 DB 동기화 → Kafka 이벤트 발행 확인
3. PERCENT 쿠폰 3개 배치 동기화 → 3개 이벤트 발행 확인
4. AMOUNT 쿠폰 3개 배치 동기화 → 3개 이벤트 발행 확인

### E2E 테스트
1. PERCENT 쿠폰 전체 플로우 → Kafka 이벤트 발행 확인
2. AMOUNT 쿠폰 전체 플로우 → Kafka 이벤트 발행 확인
3. PERCENT 쿠폰 3명 동시 발급 → 3개 이벤트 발행 확인
4. AMOUNT 쿠폰 3명 동시 발급 → 3개 이벤트 발행 확인

---

## 🎉 결론

쿠폰 도메인의 Kafka 마이그레이션이 성공적으로 완료되었으며, 다음과 같은 테스트를 통해 검증하였습니다:

✅ **12개의 테스트 케이스** 작성 완료
✅ **PERCENT/AMOUNT 타입별** 분리 테스트
✅ **단위 → 통합 → E2E** 계층별 테스트
✅ **이벤트 발행, 순서 보장, 배치 처리** 검증 완료

이제 쿠폰 발급 시 자동으로 Kafka에 `COUPON_ISSUED` 이벤트가 발행되며, 다른 서비스에서 이를 구독하여 처리할 수 있습니다.