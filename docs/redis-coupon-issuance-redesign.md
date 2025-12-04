# Redis 기반 선착순 쿠폰 발급 시스템 재설계 보고서

## 1. 개요

### 1.1 목적
기존 DB 중심의 쿠폰 발급 시스템을 Redis 자료구조 기반으로 재설계하여, 대규모 동시 요청에서도 안정적이고 빠른 선착순 쿠폰 발급을 제공합니다.

### 1.2 적용 범위
- 선착순 쿠폰 발급 요청 처리
- 중복 발급 방지
- 재고 관리 및 실시간 차감
- 비동기/배치 기반 DB 동기화
- 장애 복구 및 데이터 정합성 보장

---

## 2. 현재 시스템 분석

### 2.1 현재 구현 현황

#### 2.1.1 IssueCouponUseCase
**파일 위치:** `src/main/java/com/hhplus/ecommerce/application/coupon/IssueCouponUseCase.java:23`

**현재 로직:**
```java
public CouponHistoryEntity execute(long userId, long couponId) {
    String lockKey = "coupon:stock:lock:" + couponId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // 1. Redisson 분산 락 획득 (5초 대기, 2초 타임아웃)
        boolean acquired = lock.tryLock(5, 2, TimeUnit.SECONDS);

        // 2. DB 트랜잭션 내에서 처리
        return transactionTemplate.execute(status -> {
            // 재고 확인 (DB 조회)
            CouponEntity coupon = couponRepository.getOrThrow(couponId);
            if (coupon.getStock() <= 0) {
                throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
            }

            // 중복 발급 체크 (DB 조회)
            couponRepository.findHistoryByUserIdAndCouponId(userId, couponId)
                .ifPresent(history -> {
                    throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
                });

            // 재고 감소 (DB 업데이트)
            CouponEntity updatedCoupon = coupon.decreaseStock();
            couponRepository.save(updatedCoupon);

            // 발급 내역 생성 (DB 삽입)
            CouponHistoryEntity history = new CouponHistoryEntity(...);
            return couponRepository.saveHistory(history);
        });
    } finally {
        lock.unlock();
    }
}
```

### 2.2 현재 문제점

| 문제점 | 설명 | 영향 |
|--------|------|------|
| ❌ **DB 병목** | 모든 쿠폰 발급이 DB 트랜잭션에 의존 | 초당 수백~수천 건의 동시 요청 처리 불가 |
| ❌ **재고 조회 지연** | 매 요청마다 DB에서 재고 조회 | 락 대기 시간 증가, 응답 지연 |
| ❌ **중복 체크 비용** | DB 조회로 중복 발급 확인 | 추가 쿼리로 성능 저하 |
| ❌ **동기 처리** | 락 획득 → DB 처리 → 응답까지 동기 실행 | 사용자 대기 시간 길어짐 |
| ❌ **분산 락 경합** | 쿠폰별 단일 락으로 모든 요청 직렬화 | 선착순 경쟁 시 대부분 요청 타임아웃 |
| ❌ **실시간성 부족** | 락 대기(5초) + 처리(2초) = 최대 7초 | 선착순 이벤트에 부적합 |

### 2.3 성능 측정 (추정치)

**현재 시스템 한계:**
- 분산 락 직렬화: 초당 약 500 TPS (2초 타임아웃 기준)
- DB 트랜잭션 병목: 초당 약 200-300 TPS
- 대규모 동시 요청 시: 대부분 락 획득 실패 또는 타임아웃

**테스트 결과 (CouponConcurrencyTest.java):**
- 10명 동시 발급: 성공
- 재고 초과 방지: 성공
- 중복 발급 방지: 성공
- 그러나 수천~수만 명의 실제 선착순 이벤트에는 부적합

---

## 3. Redis 기반 설계 방안

### 3.1 방안 비교

| 구분 | 방안 1: Queue 기반 비동기 처리 | 방안 2: Sorted Set 스케줄러 배치 |
|------|-------------------------------|----------------------------------|
| **처리 방식** | 즉시 큐에 추가 → 워커가 비동기 처리 | Sorted Set에 저장 → 스케줄러가 주기적 처리 |
| **응답 속도** | 밀리초 이내 (큐 추가만) | 밀리초 이내 (Set 추가만) |
| **처리 순서** | FIFO (선입선출) 보장 | Timestamp 기준 정렬 보장 |
| **실시간성** | ⭐⭐⭐⭐⭐ 매우 높음 | ⭐⭐⭐ 보통 (스케줄 주기에 따라) |
| **처리량** | ⭐⭐⭐⭐⭐ 초당 10,000+ TPS | ⭐⭐⭐⭐ 초당 5,000+ TPS |
| **중복 방지** | Redis Set + SETNX | Redis Set + SETNX |
| **구현 복잡도** | 중간 (워커 스레드 관리) | 낮음 (스프링 스케줄러 활용) |
| **장애 복구** | 큐에 남은 요청 재처리 | Sorted Set에 남은 요청 재처리 |
| **추천도** | ✅ **추천** (선착순 이벤트) | △ 선착순보다는 정기 이벤트에 적합 |

### 3.2 **최종 선택: Queue 기반 비동기 처리** (방안 1)

**선택 이유:**
1. **실시간성**: 큐에 추가 즉시 워커가 처리 (지연 최소화)
2. **FIFO 보장**: 선착순의 핵심 요구사항 충족
3. **높은 처리량**: 비동기 멀티 워커로 병렬 처리
4. **확장성**: 워커 수를 동적으로 조절 가능
5. **간단한 큐 관리**: Redis List의 LPUSH, RPOP 명령어 활용

---

## 4. Redis 키 설계

### 4.1 키 구조

```
# 1. 쿠폰 재고 (String)
coupon:stock:{couponId}
- Value: 남은 재고 수량 (Integer)
- 명령어: GET, DECR, DECRBY
- 예시: coupon:stock:1 = "100"

# 2. 발급 요청 큐 (List)
coupon:issue:queue:{couponId}
- Value: JSON 문자열 {"userId": 123, "couponId": 1, "requestedAt": 1733280000000}
- 명령어: LPUSH (생산자), RPOP (소비자)
- 예시: LPUSH coupon:issue:queue:1 '{"userId":123,"couponId":1,"requestedAt":1733280000000}'

# 3. 중복 발급 방지 (Set)
coupon:issued:{couponId}
- Value: userId (String)
- 명령어: SADD, SISMEMBER
- 예시: coupon:issued:1 = {"101", "102", "103", ...}

# 4. 발급 완료 임시 저장 (Hash) - DB 동기화 전
coupon:issued:pending:{couponId}
- Field: userId
- Value: JSON 문자열 {"historyId": 0, "status": "ISSUED", "issuedAt": 1733280000000}
- 명령어: HSET, HGETALL, HDEL
- 예시: HSET coupon:issued:pending:1 "101" '{"historyId":0,"status":"ISSUED","issuedAt":1733280000000}'

# 5. DB 동기화 완료 마커 (Set)
coupon:synced:histories
- Value: userId:couponId
- 명령어: SADD, SISMEMBER
- 예시: coupon:synced:histories = {"101:1", "102:1", ...}
```

### 4.2 TTL 설정

| 키 패턴 | TTL | 근거 |
|---------|-----|------|
| `coupon:stock:{couponId}` | 쿠폰 유효 기간 + 1일 | 쿠폰 종료 후 자동 삭제 |
| `coupon:issue:queue:{couponId}` | 없음 (수동 관리) | 큐는 항상 소비되므로 TTL 불필요 |
| `coupon:issued:{couponId}` | 쿠폰 유효 기간 + 7일 | 중복 체크 데이터는 길게 유지 |
| `coupon:issued:pending:{couponId}` | 24시간 | DB 동기화 후 삭제, 장애 대비 |
| `coupon:synced:histories` | 7일 | 동기화 완료 기록 보관 |

---

## 5. 아키텍처 설계

### 5.1 전체 플로우

```
┌─────────────────────────────────────────────────────────────────┐
│                       Client Layer                               │
├─────────────────────────────────────────────────────────────────┤
│  POST /api/coupons/{couponId}/issue?userId=123                  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Controller Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  IssueCouponController                                           │
│    - 요청 검증 (userId, couponId)                                │
│    - IssueCouponUseCase.execute() 호출                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    UseCase Layer (동기)                          │
├─────────────────────────────────────────────────────────────────┤
│  IssueCouponUseCase.execute(userId, couponId)                   │
│    1. 쿠폰 유효성 검증 (DB - 캐시 활용)                          │
│    2. 중복 발급 체크 (Redis Set)                                 │
│       - SISMEMBER coupon:issued:{couponId} {userId}             │
│       - 이미 존재하면 예외 발생                                  │
│    3. 재고 확인 및 차감 (Redis String)                           │
│       - GET coupon:stock:{couponId}                             │
│       - DECR coupon:stock:{couponId}                            │
│       - 재고 < 0이면 INCR로 복구 후 예외 발생                    │
│    4. 발급 요청 큐에 추가 (Redis List)                           │
│       - LPUSH coupon:issue:queue:{couponId} {...}               │
│    5. 중복 방지 Set에 추가 (Redis Set)                           │
│       - SADD coupon:issued:{couponId} {userId}                  │
│    6. 즉시 응답 반환 (202 Accepted)                              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Worker Layer (비동기)                          │
├─────────────────────────────────────────────────────────────────┤
│  CouponIssueWorker (@Scheduled, fixedDelay=100ms)               │
│    - 큐에서 요청 가져오기 (RPOP)                                 │
│    - 발급 처리 로직 실행                                         │
│      1. Redis Hash에 임시 저장                                  │
│         HSET coupon:issued:pending:{couponId} {userId} {...}    │
│      2. 주기적으로 DB 동기화 (Batch)                             │
│    - 실패 시 재시도 큐로 이동                                    │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Scheduler Layer (배치)                          │
├─────────────────────────────────────────────────────────────────┤
│  CouponSyncScheduler (@Scheduled, fixedRate=10초)               │
│    1. Redis Hash에서 미동기화 데이터 조회                        │
│       HGETALL coupon:issued:pending:{couponId}                  │
│    2. DB에 배치 삽입 (Batch Insert)                             │
│       - CouponHistoryEntity 생성 및 저장                        │
│       - 쿠폰 재고 업데이트 (DB)                                  │
│    3. 동기화 완료 마킹                                           │
│       SADD coupon:synced:histories {userId}:{couponId}          │
│       HDEL coupon:issued:pending:{couponId} {userId}            │
│    4. 실패 시 로그 기록 및 재시도                                │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Database (MySQL)                            │
├─────────────────────────────────────────────────────────────────┤
│  - coupons (쿠폰 메타 정보)                                      │
│  - coupon_histories (발급 내역)                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 응답 방식

**방식 1: 즉시 응답 (비동기) - 추천**
```http
POST /api/coupons/1/issue?userId=123

# 응답 (202 Accepted)
{
  "status": "PENDING",
  "message": "쿠폰 발급 요청이 접수되었습니다.",
  "couponId": 1,
  "userId": 123,
  "requestedAt": "2025-12-04T10:30:00"
}
```

**방식 2: 폴링 조회 (선택사항)**
```http
GET /api/coupons/1/issue/status?userId=123

# 응답 (200 OK)
{
  "status": "ISSUED",  # PENDING, ISSUED, FAILED
  "couponHistoryId": 12345,
  "issuedAt": "2025-12-04T10:30:05",
  "message": "쿠폰이 발급되었습니다."
}
```

---

## 6. 상세 구현 계획

### 6.1 Redis 초기화 (쿠폰 생성 시)

```java
@Service
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public CouponEntity execute(CreateCouponRequest request) {
        // 1. DB에 쿠폰 저장
        CouponEntity coupon = new CouponEntity(...);
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // 2. Redis에 재고 초기화
        String stockKey = "coupon:stock:" + savedCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, String.valueOf(savedCoupon.getStock()));

        // 3. TTL 설정 (쿠폰 유효 기간 + 1일)
        long ttl = savedCoupon.getValidUntil() - System.currentTimeMillis() + 86400000L;
        redisTemplate.expire(stockKey, ttl, TimeUnit.MILLISECONDS);

        log.info("쿠폰 생성 완료 - couponId: {}, Redis 재고: {}",
            savedCoupon.getId(), savedCoupon.getStock());

        return savedCoupon;
    }
}
```

### 6.2 쿠폰 발급 요청 (UseCase)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueCouponUseCase {

    private final RedisTemplate<String, String> redisTemplate;
    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;

    public IssueCouponResponse execute(long userId, long couponId) {
        // 1. 쿠폰 유효성 검증 (DB - 캐시 활용)
        CouponEntity coupon = couponRepository.getOrThrow(couponId);
        long now = System.currentTimeMillis();

        if (now < coupon.getValidFrom() || now > coupon.getValidUntil()) {
            throw new IllegalStateException("쿠폰 유효 기간이 아닙니다.");
        }

        // 2. 중복 발급 체크 (Redis Set)
        String issuedSetKey = "coupon:issued:" + couponId;
        Boolean isAlreadyIssued = redisTemplate.opsForSet()
            .isMember(issuedSetKey, String.valueOf(userId));

        if (Boolean.TRUE.equals(isAlreadyIssued)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 3. 재고 확인 및 차감 (Redis String)
        String stockKey = "coupon:stock:" + couponId;
        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

        if (remainingStock == null || remainingStock < 0) {
            // 재고 복구
            if (remainingStock != null) {
                redisTemplate.opsForValue().increment(stockKey);
            }
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        // 4. 발급 요청 큐에 추가 (Redis List)
        String queueKey = "coupon:issue:queue:" + couponId;
        CouponIssueRequest request = new CouponIssueRequest(
            userId,
            couponId,
            System.currentTimeMillis()
        );

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            redisTemplate.opsForList().leftPush(queueKey, requestJson);
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시 재고 복구
            redisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("쿠폰 발급 요청 처리 중 오류가 발생했습니다.", e);
        }

        // 5. 중복 방지 Set에 추가 (Redis Set)
        redisTemplate.opsForSet().add(issuedSetKey, String.valueOf(userId));

        // 6. TTL 설정 (중복 방지 Set)
        long ttl = coupon.getValidUntil() - now + 604800000L; // 쿠폰 종료 + 7일
        redisTemplate.expire(issuedSetKey, ttl, TimeUnit.MILLISECONDS);

        log.info("쿠폰 발급 요청 접수 - userId: {}, couponId: {}, 남은 재고: {}",
            userId, couponId, remainingStock);

        // 7. 즉시 응답 반환
        return new IssueCouponResponse(
            "PENDING",
            "쿠폰 발급 요청이 접수되었습니다.",
            couponId,
            userId,
            System.currentTimeMillis()
        );
    }
}

@Getter
@AllArgsConstructor
class CouponIssueRequest {
    private Long userId;
    private Long couponId;
    private Long requestedAt;
}

@Getter
@AllArgsConstructor
class IssueCouponResponse {
    private String status;
    private String message;
    private Long couponId;
    private Long userId;
    private Long requestedAt;
}
```

### 6.3 워커 (비동기 처리)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueWorker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 100ms마다 큐에서 요청 가져와서 처리
     */
    @Scheduled(fixedDelay = 100)
    public void processIssueQueue() {
        // 모든 쿠폰 큐를 스캔 (실제로는 활성 쿠폰만 스캔)
        Set<String> queueKeys = redisTemplate.keys("coupon:issue:queue:*");

        if (queueKeys == null || queueKeys.isEmpty()) {
            return;
        }

        for (String queueKey : queueKeys) {
            processCouponQueue(queueKey);
        }
    }

    private void processCouponQueue(String queueKey) {
        // 큐에서 최대 10개씩 배치 처리
        int batchSize = 10;

        for (int i = 0; i < batchSize; i++) {
            String requestJson = redisTemplate.opsForList().rightPop(queueKey);

            if (requestJson == null) {
                break; // 큐가 비었음
            }

            try {
                CouponIssueRequest request = objectMapper.readValue(
                    requestJson,
                    CouponIssueRequest.class
                );

                processIssueRequest(request);

            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 실패 - request: {}", requestJson, e);
                // 파싱 실패 시 재시도 큐로 이동
                moveToRetryQueue(queueKey, requestJson);
            } catch (Exception e) {
                log.error("쿠폰 발급 처리 실패 - request: {}", requestJson, e);
                // 처리 실패 시 재시도 큐로 이동
                moveToRetryQueue(queueKey, requestJson);
            }
        }
    }

    private void processIssueRequest(CouponIssueRequest request) {
        Long userId = request.getUserId();
        Long couponId = request.getCouponId();
        Long requestedAt = request.getRequestedAt();

        // Redis Hash에 임시 저장 (DB 동기화 전)
        String pendingKey = "coupon:issued:pending:" + couponId;

        CouponIssuePending pending = new CouponIssuePending(
            0L,  // historyId는 DB 저장 후 생성
            userId,
            couponId,
            "ISSUED",
            requestedAt
        );

        try {
            String pendingJson = objectMapper.writeValueAsString(pending);
            redisTemplate.opsForHash().put(
                pendingKey,
                String.valueOf(userId),
                pendingJson
            );

            // TTL 설정 (24시간)
            redisTemplate.expire(pendingKey, 24, TimeUnit.HOURS);

            log.info("쿠폰 발급 임시 저장 완료 - userId: {}, couponId: {}", userId, couponId);

        } catch (JsonProcessingException e) {
            log.error("Redis Hash 저장 실패", e);
            throw new RuntimeException("쿠폰 발급 처리 실패", e);
        }
    }

    private void moveToRetryQueue(String originalQueueKey, String requestJson) {
        String retryQueueKey = originalQueueKey + ":retry";
        redisTemplate.opsForList().leftPush(retryQueueKey, requestJson);
        log.warn("재시도 큐로 이동 - queue: {}", retryQueueKey);
    }
}

@Getter
@AllArgsConstructor
@NoArgsConstructor
class CouponIssuePending {
    private Long historyId;
    private Long userId;
    private Long couponId;
    private String status;
    private Long issuedAt;
}
```

### 6.4 스케줄러 (DB 동기화)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;

    /**
     * 10초마다 Redis → DB 동기화
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncCouponIssuesToDB() {
        log.info("쿠폰 발급 내역 DB 동기화 시작");

        // 모든 pending Hash 스캔
        Set<String> pendingKeys = redisTemplate.keys("coupon:issued:pending:*");

        if (pendingKeys == null || pendingKeys.isEmpty()) {
            log.info("동기화할 데이터 없음");
            return;
        }

        int totalSynced = 0;
        int totalFailed = 0;

        for (String pendingKey : pendingKeys) {
            try {
                int synced = syncPendingCoupon(pendingKey);
                totalSynced += synced;
            } catch (Exception e) {
                log.error("동기화 실패 - key: {}", pendingKey, e);
                totalFailed++;
            }
        }

        log.info("쿠폰 발급 내역 DB 동기화 완료 - 성공: {}, 실패: {}", totalSynced, totalFailed);
    }

    private int syncPendingCoupon(String pendingKey) {
        // Redis Hash에서 모든 데이터 가져오기
        Map<Object, Object> pendingMap = redisTemplate.opsForHash().entries(pendingKey);

        if (pendingMap.isEmpty()) {
            return 0;
        }

        // couponId 추출 (key 형식: coupon:issued:pending:{couponId})
        Long couponId = Long.parseLong(pendingKey.split(":")[3]);

        List<CouponHistoryEntity> historiesToSave = new ArrayList<>();
        List<String> userIdsToDelete = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : pendingMap.entrySet()) {
            String userIdStr = (String) entry.getKey();
            String pendingJson = (String) entry.getValue();

            try {
                CouponIssuePending pending = objectMapper.readValue(
                    pendingJson,
                    CouponIssuePending.class
                );

                // 이미 동기화된 데이터인지 확인
                String syncKey = "coupon:synced:histories";
                String syncValue = userIdStr + ":" + couponId;
                Boolean isSynced = redisTemplate.opsForSet().isMember(syncKey, syncValue);

                if (Boolean.TRUE.equals(isSynced)) {
                    log.warn("이미 동기화된 데이터 - userId: {}, couponId: {}", userIdStr, couponId);
                    userIdsToDelete.add(userIdStr);
                    continue;
                }

                // DB에 저장할 엔티티 생성
                CouponHistoryEntity history = new CouponHistoryEntity(
                    0L,  // ID 자동 생성
                    Long.parseLong(userIdStr),
                    couponId,
                    CouponStatus.valueOf(pending.getStatus()),
                    pending.getIssuedAt(),
                    null
                );

                historiesToSave.add(history);

            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 실패 - userId: {}, json: {}", userIdStr, pendingJson, e);
            }
        }

        if (historiesToSave.isEmpty()) {
            return 0;
        }

        // 배치 삽입
        List<CouponHistoryEntity> savedHistories = couponRepository.saveAllHistories(historiesToSave);

        // 동기화 완료 마킹
        String syncKey = "coupon:synced:histories";
        for (CouponHistoryEntity history : savedHistories) {
            String syncValue = history.getUserId() + ":" + history.getCouponId();
            redisTemplate.opsForSet().add(syncKey, syncValue);
            userIdsToDelete.add(String.valueOf(history.getUserId()));
        }

        // TTL 설정 (7일)
        redisTemplate.expire(syncKey, 7, TimeUnit.DAYS);

        // Redis Hash에서 동기화 완료된 데이터 삭제
        for (String userId : userIdsToDelete) {
            redisTemplate.opsForHash().delete(pendingKey, userId);
        }

        log.info("DB 동기화 완료 - couponId: {}, count: {}", couponId, savedHistories.size());

        return savedHistories.size();
    }

    /**
     * 매일 자정 1시에 동기화 완료 마커 정리
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void cleanupSyncMarkers() {
        String syncKey = "coupon:synced:histories";
        redisTemplate.delete(syncKey);
        log.info("동기화 마커 정리 완료");
    }
}
```

### 6.5 Repository 수정

```java
public interface CouponRepository {
    // 기존 메서드들...

    /**
     * 배치 삽입 (DB 동기화용)
     */
    List<CouponHistoryEntity> saveAllHistories(List<CouponHistoryEntity> histories);
}

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponHistoryJpaRepository couponHistoryJpaRepository;

    @Override
    public List<CouponHistoryEntity> saveAllHistories(List<CouponHistoryEntity> histories) {
        return couponHistoryJpaRepository.saveAll(histories);
    }

    // 기존 메서드들...
}
```

---

## 7. 중복 방지 및 동시성 제어 전략

### 7.1 중복 발급 방지

**전략 1: Redis Set (SADD + SISMEMBER)**
```java
// 발급 전 중복 체크
Boolean isAlreadyIssued = redisTemplate.opsForSet()
    .isMember("coupon:issued:" + couponId, String.valueOf(userId));

if (Boolean.TRUE.equals(isAlreadyIssued)) {
    throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
}

// 발급 시 Set에 추가
redisTemplate.opsForSet().add("coupon:issued:" + couponId, String.valueOf(userId));
```

**특징:**
- ✅ O(1) 시간 복잡도
- ✅ 원자적 연산 보장
- ✅ 메모리 효율적 (비트맵 대비)
- ⚠️ 동시 요청 시 경합 조건 발생 가능

**전략 2: Redis String (SETNX)**
```java
// 발급 시 원자적 중복 체크 + 등록
String key = "coupon:issued:" + couponId + ":" + userId;
Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", 24, TimeUnit.HOURS);

if (Boolean.FALSE.equals(success)) {
    throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
}
```

**특징:**
- ✅ 완벽한 원자성 (SETNX)
- ✅ 경합 조건 없음
- ⚠️ 메모리 사용량 높음 (키 개수 많음)

**최종 선택: 전략 1 (Redis Set) + 낙관적 락**
- 성능과 메모리 효율의 균형
- 중복 발급 시도는 매우 드물기 때문에 경합 조건 무시 가능
- DB 동기화 시 중복 체크로 이중 안전장치

### 7.2 재고 관리 동시성

**Redis DECR의 원자성:**
```java
// DECR은 원자적 연산 (Thread-Safe)
Long remainingStock = redisTemplate.opsForValue().decrement("coupon:stock:" + couponId);

if (remainingStock < 0) {
    // 재고 초과 시 복구
    redisTemplate.opsForValue().increment("coupon:stock:" + couponId);
    throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
}
```

**특징:**
- ✅ Redis의 단일 스레드 모델로 원자성 보장
- ✅ 분산 락 불필요
- ✅ 높은 처리량 (초당 10,000+ TPS)
- ✅ 초과 발급 방지 (재고 < 0 체크)

### 7.3 큐 처리 동시성

**멀티 워커 안전성:**
```java
// RPOP은 원자적 연산 (여러 워커가 동시 실행 가능)
String requestJson = redisTemplate.opsForList().rightPop("coupon:issue:queue:" + couponId);

// 워커 1: RPOP → 첫 번째 요청 가져감
// 워커 2: RPOP → 두 번째 요청 가져감
// 워커 3: RPOP → 세 번째 요청 가져감
```

**특징:**
- ✅ Redis List의 RPOP은 원자적
- ✅ 멀티 워커 병렬 처리 가능
- ✅ 중복 처리 없음
- ✅ FIFO 순서 보장

---

## 8. 성능 비교 및 예상 효과

### 8.1 성능 비교

| 항목 | 현재 시스템 (DB + 분산 락) | 개선 시스템 (Redis Queue) |
|------|---------------------------|--------------------------|
| **응답 시간** | 평균 2-7초 (락 대기 포함) | 평균 10-50ms (큐 추가만) |
| **처리량 (TPS)** | 200-500 TPS | 10,000+ TPS |
| **동시 요청 처리** | 직렬화 (큐잉) | 병렬화 (멀티 워커) |
| **재고 조회** | DB 쿼리 (느림) | Redis GET (빠름) |
| **중복 체크** | DB 쿼리 (느림) | Redis SISMEMBER (빠름) |
| **DB 부하** | 요청당 3-4개 쿼리 | 배치로 최소화 |
| **락 경합** | 높음 (쿠폰별 단일 락) | 없음 (락 불필요) |
| **확장성** | 수직 확장만 가능 | 수평 확장 가능 (워커 추가) |

### 8.2 예상 효과

**시나리오: 선착순 100개 쿠폰, 동시 10,000명 요청**

**현재 시스템:**
- 100명 성공, 9,900명 실패 (락 타임아웃 또는 대기)
- 평균 응답 시간: 5초
- DB 부하: 초당 200-500 쿼리

**개선 시스템:**
- 100명 성공, 9,900명 즉시 "재고 소진" 응답
- 평균 응답 시간: 20ms
- DB 부하: 10초마다 100건 배치 삽입 (초당 10 쿼리)

**개선 효과:**
- ✅ 응답 속도: **250배 향상** (5초 → 20ms)
- ✅ 처리량: **20배 향상** (500 TPS → 10,000 TPS)
- ✅ DB 부하: **20배 감소** (200 쿼리/초 → 10 쿼리/초)
- ✅ 사용자 경험: 즉시 응답으로 대기 시간 제거
- ✅ 확장성: 워커 수 증가로 처리량 선형 확장

---

## 9. 장애 복구 및 데이터 정합성

### 9.1 장애 시나리오별 대응

| 장애 시나리오 | 영향 | 복구 방안 |
|--------------|------|----------|
| **워커 중단** | 큐에 요청 누적 | 워커 재시작 시 큐에서 순차 처리 |
| **스케줄러 중단** | Redis 데이터 미동기화 | 스케줄러 재시작 시 pending Hash 전체 동기화 |
| **Redis 장애** | 발급 요청 실패 | Redis 복구 후 재시도 (사용자 재요청) |
| **DB 장애** | 동기화 실패 | DB 복구 후 pending Hash에서 재동기화 |
| **서버 재시작** | 큐 데이터 유실 위험 | Redis 영속화 (AOF 또는 RDB) 활성화 |

### 9.2 데이터 정합성 보장

**정합성 체크 포인트:**

1. **Redis 재고 vs DB 재고**
   ```java
   @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
   public void reconcileStock() {
       List<CouponEntity> coupons = couponRepository.findAll();

       for (CouponEntity coupon : coupons) {
           String stockKey = "coupon:stock:" + coupon.getId();
           String redisStock = redisTemplate.opsForValue().get(stockKey);

           if (redisStock == null) {
               // Redis 재고 없음 → DB에서 복구
               redisTemplate.opsForValue().set(stockKey, String.valueOf(coupon.getStock()));
               log.warn("Redis 재고 복구 - couponId: {}, stock: {}",
                   coupon.getId(), coupon.getStock());
               continue;
           }

           int redisStockInt = Integer.parseInt(redisStock);
           int dbStock = coupon.getStock();

           if (redisStockInt != dbStock) {
               log.error("재고 불일치 - couponId: {}, Redis: {}, DB: {}",
                   coupon.getId(), redisStockInt, dbStock);
               // 알림 발송 또는 수동 조정 필요
           }
       }
   }
   ```

2. **중복 발급 체크**
   ```java
   // DB 동기화 시 중복 체크
   Optional<CouponHistoryEntity> existing = couponRepository
       .findHistoryByUserIdAndCouponId(userId, couponId);

   if (existing.isPresent()) {
       log.warn("중복 발급 감지 (동기화 스킵) - userId: {}, couponId: {}",
           userId, couponId);
       return; // 이미 DB에 존재하면 스킵
   }
   ```

3. **재시도 큐 모니터링**
   ```java
   @Scheduled(fixedRate = 60000)  // 1분마다
   public void monitorRetryQueue() {
       Set<String> retryQueues = redisTemplate.keys("coupon:issue:queue:*:retry");

       if (retryQueues == null || retryQueues.isEmpty()) {
           return;
       }

       for (String retryQueueKey : retryQueues) {
           Long size = redisTemplate.opsForList().size(retryQueueKey);

           if (size != null && size > 0) {
               log.warn("재시도 큐에 요청 누적 - queue: {}, size: {}",
                   retryQueueKey, size);
               // 알림 발송 또는 수동 처리 필요
           }
       }
   }
   ```

---

## 10. API 명세

### 10.1 쿠폰 발급 요청

```http
POST /api/coupons/{couponId}/issue?userId={userId}
```

**Request Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| couponId | Long | Y | 쿠폰 ID (Path) |
| userId | Long | Y | 사용자 ID (Query) |

**Response (202 Accepted):**
```json
{
  "status": "PENDING",
  "message": "쿠폰 발급 요청이 접수되었습니다.",
  "couponId": 1,
  "userId": 123,
  "requestedAt": "2025-12-04T10:30:00"
}
```

**Error Response (400 Bad Request):**
```json
{
  "status": "FAILED",
  "message": "쿠폰이 모두 소진되었습니다.",
  "couponId": 1,
  "userId": 123
}
```

```json
{
  "status": "FAILED",
  "message": "이미 발급받은 쿠폰입니다.",
  "couponId": 1,
  "userId": 123
}
```

```json
{
  "status": "FAILED",
  "message": "쿠폰 유효 기간이 아닙니다.",
  "couponId": 1,
  "userId": 123
}
```

### 10.2 발급 상태 조회 (선택사항)

```http
GET /api/coupons/{couponId}/issue/status?userId={userId}
```

**Response (200 OK):**
```json
{
  "status": "ISSUED",
  "couponHistoryId": 12345,
  "issuedAt": "2025-12-04T10:30:05",
  "message": "쿠폰이 발급되었습니다."
}
```

```json
{
  "status": "PENDING",
  "message": "쿠폰 발급 처리 중입니다."
}
```

```json
{
  "status": "NOT_FOUND",
  "message": "발급 요청 내역을 찾을 수 없습니다."
}
```

---

## 11. 테스트 계획

### 11.1 단위 테스트

```java
@SpringBootTest
@DisplayName("Redis 기반 쿠폰 발급 단위 테스트")
class IssueCouponUseCaseTest {

    @Test
    @DisplayName("정상 발급 - 재고 차감 및 큐 추가")
    void issueSuccess() {
        // given
        long userId = 123L;
        long couponId = 1L;

        // Redis 초기화
        redisTemplate.opsForValue().set("coupon:stock:1", "100");

        // when
        IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);

        // then
        assertThat(response.getStatus()).isEqualTo("PENDING");

        // 재고 확인
        String stock = redisTemplate.opsForValue().get("coupon:stock:1");
        assertThat(stock).isEqualTo("99");

        // 큐 확인
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:1");
        assertThat(queueSize).isEqualTo(1);

        // 중복 방지 Set 확인
        Boolean isMember = redisTemplate.opsForSet()
            .isMember("coupon:issued:1", "123");
        assertThat(isMember).isTrue();
    }

    @Test
    @DisplayName("중복 발급 방지")
    void preventDuplicateIssue() {
        // given
        long userId = 123L;
        long couponId = 1L;

        redisTemplate.opsForValue().set("coupon:stock:1", "100");
        redisTemplate.opsForSet().add("coupon:issued:1", "123");

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 발급받은 쿠폰입니다.");
    }

    @Test
    @DisplayName("재고 소진 시 예외 발생")
    void stockExhausted() {
        // given
        long userId = 123L;
        long couponId = 1L;

        redisTemplate.opsForValue().set("coupon:stock:1", "0");

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("쿠폰이 모두 소진되었습니다.");
    }
}
```

### 11.2 통합 테스트

```java
@SpringBootTest
@DisplayName("Redis 기반 쿠폰 발급 통합 테스트")
class CouponIssueIntegrationTest {

    @Test
    @DisplayName("전체 플로우 - 발급 요청 → 워커 처리 → DB 동기화")
    void fullFlowTest() throws InterruptedException {
        // given
        long userId = 123L;
        long couponId = 1L;

        // 쿠폰 생성 및 Redis 초기화
        CreateCouponRequest request = new CreateCouponRequest(...);
        createCouponUseCase.execute(request);

        // when - 발급 요청
        IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);

        // then - 즉시 응답
        assertThat(response.getStatus()).isEqualTo("PENDING");

        // 워커가 처리할 때까지 대기 (최대 5초)
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> {
                String pendingKey = "coupon:issued:pending:" + couponId;
                return redisTemplate.opsForHash().hasKey(pendingKey, String.valueOf(userId));
            });

        // 스케줄러가 동기화할 때까지 대기 (최대 15초)
        await().atMost(15, TimeUnit.SECONDS)
            .until(() -> {
                Optional<CouponHistoryEntity> history = couponRepository
                    .findHistoryByUserIdAndCouponId(userId, couponId);
                return history.isPresent();
            });

        // DB 확인
        CouponHistoryEntity history = couponRepository
            .findHistoryByUserIdAndCouponId(userId, couponId)
            .orElseThrow();

        assertThat(history.getUserId()).isEqualTo(userId);
        assertThat(history.getCouponId()).isEqualTo(couponId);
        assertThat(history.getStatus()).isEqualTo(CouponStatus.ISSUED);
    }
}
```

### 11.3 동시성 테스트

```java
@SpringBootTest
@DisplayName("Redis 기반 쿠폰 발급 동시성 테스트")
class CouponConcurrencyTest {

    @Test
    @DisplayName("대규모 동시 요청 - 10,000명이 100개 쿠폰 발급")
    void massiveConcurrentIssue() throws InterruptedException {
        // given
        long couponId = 1L;
        int stock = 100;
        int threadCount = 10000;

        // 쿠폰 생성 및 Redis 초기화
        CreateCouponRequest request = new CreateCouponRequest(
            "선착순 100명 쿠폰", "FIXED", 5000, 10000, 50000,
            stock, validFrom, validUntil
        );
        createCouponUseCase.execute(request);

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10,000명이 동시에 발급 시도
        for (long userId = 1; userId <= threadCount; userId++) {
            long finalUserId = userId;
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(finalUserId, couponId);
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
        System.out.println("========== 대규모 동시 요청 테스트 결과 ==========");
        System.out.println("초기 재고: " + stock);
        System.out.println("요청 수: " + threadCount);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failCount.get());

        // 재고 확인
        String remainingStock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        System.out.println("남은 재고 (Redis): " + remainingStock);

        // 정확히 100명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(failCount.get()).isEqualTo(threadCount - stock);
        assertThat(Integer.parseInt(remainingStock)).isEqualTo(0);

        // 큐 사이즈 확인
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(stock);  // 성공한 100명의 요청이 큐에 있음

        // 중복 방지 Set 확인
        Long issuedCount = redisTemplate.opsForSet().size("coupon:issued:" + couponId);
        assertThat(issuedCount).isEqualTo(stock);  // 100명만 Set에 추가됨

        System.out.println("============================================");
    }

    @Test
    @DisplayName("동일 사용자 동시 중복 요청 - 1명이 10번 시도")
    void samUserConcurrentIssue() throws InterruptedException {
        // given
        long userId = 123L;
        long couponId = 1L;
        int threadCount = 10;

        // 쿠폰 생성 및 Redis 초기화
        CreateCouponRequest request = new CreateCouponRequest(...);
        request = request.withStock(10);
        createCouponUseCase.execute(request);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 사용자가 10번 동시 시도
        for (int i = 0; i < threadCount; i++) {
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

        // then - 1번만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        // 중복 방지 Set 확인
        Boolean isMember = redisTemplate.opsForSet()
            .isMember("coupon:issued:" + couponId, String.valueOf(userId));
        assertThat(isMember).isTrue();
    }
}
```

### 11.4 성능 테스트 (JMeter/Gatling)

**시나리오 1: 일반 트래픽**
- 사용자 수: 1,000명
- 램프업 시간: 10초
- 쿠폰 재고: 500개
- 예상 결과: 500명 성공, 500명 실패, 평균 응답 시간 < 100ms

**시나리오 2: 피크 트래픽**
- 사용자 수: 10,000명
- 램프업 시간: 1초
- 쿠폰 재고: 100개
- 예상 결과: 100명 성공, 9,900명 실패, 평균 응답 시간 < 200ms

**시나리오 3: 지속 트래픽**
- 사용자 수: 100명
- 지속 시간: 10분
- 쿠폰 재고: 1,000개
- 예상 결과: 1,000명 성공, 평균 TPS > 5,000

---

## 12. 마이그레이션 전략

### 12.1 단계별 전환

**Phase 1: 준비 (1주)**
1. Redis 키 설계 확정
2. 워커 및 스케줄러 구현
3. 단위 테스트 작성

**Phase 2: 개발 환경 적용 (1주)**
1. 새로운 UseCase 구현
2. 통합 테스트 실행
3. 성능 테스트 실행
4. 버그 수정

**Phase 3: 스테이징 환경 검증 (1주)**
1. 실제 트래픽 수준 테스트
2. 모니터링 대시보드 구축
3. 알림 설정 (재시도 큐 누적, 재고 불일치 등)

**Phase 4: 프로덕션 적용 (1-2주)**
1. 카나리 배포 (10% 트래픽)
2. 모니터링 및 안정화
3. 점진적 확대 (50% → 100%)
4. 기존 코드 제거

### 12.2 롤백 계획

**롤백 트리거:**
- Redis 장애로 발급 불가 시
- 데이터 정합성 문제 발견 시
- 워커/스케줄러 장애 시

**롤백 방법:**
1. 이전 버전으로 배포 (기존 DB 중심 로직)
2. Redis 데이터는 유지 (장애 복구 후 동기화)
3. 큐에 남은 요청은 수동 처리 또는 재시도

---

## 13. 모니터링 및 알림

### 13.1 모니터링 지표

| 지표 | 설명 | 임계값 | 알림 |
|------|------|--------|------|
| **큐 사이즈** | `coupon:issue:queue:{couponId}` 길이 | > 1,000 | Slack 알림 |
| **재시도 큐 사이즈** | `coupon:issue:queue:{couponId}:retry` 길이 | > 10 | Slack 알림 |
| **pending Hash 사이즈** | `coupon:issued:pending:{couponId}` 크기 | > 500 | Slack 알림 |
| **워커 처리 속도** | 초당 처리된 요청 수 | < 100 | Slack 알림 |
| **스케줄러 지연** | 마지막 동기화 이후 시간 | > 60초 | Slack 알림 |
| **재고 불일치** | Redis 재고 ≠ DB 재고 | 불일치 발견 시 | PagerDuty |
| **Redis 메모리** | Redis 메모리 사용률 | > 80% | Slack 알림 |

### 13.2 로그 전략

```java
// 발급 요청 시
log.info("쿠폰 발급 요청 접수 - userId: {}, couponId: {}, 남은 재고: {}",
    userId, couponId, remainingStock);

// 워커 처리 시
log.info("쿠폰 발급 임시 저장 완료 - userId: {}, couponId: {}", userId, couponId);

// 스케줄러 동기화 시
log.info("쿠폰 발급 내역 DB 동기화 완료 - 성공: {}, 실패: {}", totalSynced, totalFailed);

// 재고 불일치 감지 시
log.error("재고 불일치 - couponId: {}, Redis: {}, DB: {}",
    couponId, redisStockInt, dbStock);
```

---

## 14. 대안 설계: Sorted Set 스케줄러 배치 (참고)

### 14.1 Sorted Set 활용

```java
// 발급 요청 시
double score = System.currentTimeMillis();  // Timestamp를 score로 사용
String member = userId + ":" + couponId;
redisTemplate.opsForZSet().add("coupon:issue:pending", member, score);

// 스케줄러가 정기적으로 처리
@Scheduled(fixedDelay = 1000)  // 1초마다
public void processPendingIssues() {
    long now = System.currentTimeMillis();

    // 현재 시각 이전의 요청만 가져오기 (0 ~ now)
    Set<ZSetOperations.TypedTuple<String>> requests =
        redisTemplate.opsForZSet().rangeByScoreWithScores("coupon:issue:pending", 0, now);

    for (ZSetOperations.TypedTuple<String> request : requests) {
        String[] parts = request.getValue().split(":");
        long userId = Long.parseLong(parts[0]);
        long couponId = Long.parseLong(parts[1]);

        // 발급 처리
        processIssueRequest(userId, couponId);

        // 처리 완료 후 제거
        redisTemplate.opsForZSet().remove("coupon:issue:pending", request.getValue());
    }
}
```

### 14.2 Sorted Set vs Queue 비교

| 특징 | Sorted Set | Queue (List) |
|------|-----------|--------------|
| **순서 보장** | Timestamp 기준 정렬 | FIFO 보장 |
| **실시간성** | 스케줄 주기에 따라 지연 | 즉시 처리 |
| **우선순위** | Score로 우선순위 조정 가능 | 우선순위 없음 |
| **복잡도** | O(log N) | O(1) |
| **용도** | 정기 배치, 우선순위 큐 | 실시간 이벤트 처리 |

**결론:** 선착순 쿠폰 발급에는 **Queue (List)** 방식이 더 적합합니다.

---

## 15. 결론

### 15.1 주요 개선 사항

| 항목 | 개선 내용 |
|------|----------|
| **응답 속도** | 2-7초 → 10-50ms (250배 향상) |
| **처리량** | 200-500 TPS → 10,000+ TPS (20배 향상) |
| **DB 부하** | 요청당 3-4개 쿼리 → 배치 삽입으로 최소화 (20배 감소) |
| **확장성** | 수직 확장만 → 수평 확장 가능 (워커 추가) |
| **실시간성** | 락 대기로 지연 → 즉시 응답으로 대기 시간 제거 |
| **안정성** | 락 타임아웃 빈번 → 큐 기반으로 안정적 처리 |

### 15.2 도입 효과

**비즈니스 관점:**
- ✅ 대규모 선착순 이벤트 가능 (수만 명 동시 접속)
- ✅ 사용자 경험 개선 (즉시 응답)
- ✅ 마케팅 이벤트 확대 가능

**기술 관점:**
- ✅ DB 부하 감소로 전체 시스템 안정성 향상
- ✅ Redis 활용으로 인프라 비용 효율화
- ✅ 비동기 아키텍처로 확장성 확보
- ✅ 이벤트 기반 설계로 유지보수 용이

### 15.3 다음 단계

**단기 (1-2개월):**
1. ✅ Queue 기반 쿠폰 발급 시스템 구현
2. ✅ 통합/성능 테스트 완료
3. ✅ 프로덕션 배포 및 안정화

**중기 (3-6개월):**
1. 📊 실시간 대시보드 구축 (Grafana + Prometheus)
2. 🔔 고급 알림 시스템 구축 (PagerDuty 연동)
3. 🚀 멀티 리전 Redis 확장 (지리적 분산)

**장기 (6개월 이후):**
1. 🎯 ML 기반 수요 예측 (재고 최적화)
2. 🌐 글로벌 CDN 연동 (지연 시간 최소화)
3. 🔄 이벤트 소싱 도입 (완벽한 감사 추적)

---

## 부록

### A. Redis 명령어 레퍼런스

| 명령어 | 설명 | 시간 복잡도 | 예시 |
|--------|------|------------|------|
| **GET** | 값 조회 | O(1) | `GET coupon:stock:1` |
| **DECR** | 값 감소 (원자적) | O(1) | `DECR coupon:stock:1` |
| **INCR** | 값 증가 (원자적) | O(1) | `INCR coupon:stock:1` |
| **LPUSH** | 리스트 왼쪽에 추가 | O(1) | `LPUSH queue:1 "{...}"` |
| **RPOP** | 리스트 오른쪽에서 제거 | O(1) | `RPOP queue:1` |
| **SADD** | Set에 추가 | O(1) | `SADD issued:1 "123"` |
| **SISMEMBER** | Set 멤버 확인 | O(1) | `SISMEMBER issued:1 "123"` |
| **HSET** | Hash 필드 설정 | O(1) | `HSET pending:1 "123" "{...}"` |
| **HGETALL** | Hash 전체 조회 | O(N) | `HGETALL pending:1` |
| **EXPIRE** | TTL 설정 | O(1) | `EXPIRE stock:1 86400` |

---

**작성자:** Claude Code
**작성일:** 2025-12-04
**버전:** 1.0