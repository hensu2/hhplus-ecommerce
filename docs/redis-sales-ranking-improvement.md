# Redis 기반 실시간 판매 랭킹 시스템 개선 보고서

## 1. 개요

### 1.1 목적
기존의 일간 인기상품 기능을 실시간 판매 랭킹 시스템으로 개선하여, 주문 생성/취소 시 자동으로 랭킹을 업데이트하고 금일/금주 단위의 판매 랭킹을 제공합니다.

### 1.2 적용 범위
- 주문 생성 시 판매 랭킹 업데이트
- 주문 취소 시 판매 랭킹 차감
- 금일/금주 판매 랭킹 조회 API
- 비동기 처리를 통한 성능 최적화

---

## 2. 현재 구조 분석

### 2.1 기존 구현 현황

#### 2.1.1 ProductStatisticsRepositoryImpl
**파일 위치:** `src/main/java/com/hhplus/ecommerce/infrastructure/product/ProductStatisticsRepositoryImpl.java`

**현재 Redis 키 구조:**
```
product:stats:{productId}          # Hash - 상품별 통계 (조회수, 판매수)
daily:popular:products             # Sorted Set - 일간 인기상품 (고정 키)
```

**주요 메서드:**
- `save()`: Hash에 viewCount, salesCount, updatedAt 저장
- `findByProductId()`: 특정 상품의 통계 조회
- `findAll()`: `daily:popular:products` Sorted Set에서 전체 조회

#### 2.1.2 GetPopularProductsUseCase
**파일 위치:** `src/main/java/com/hhplus/ecommerce/application/product/GetPopularProductsUseCase.java`

**현재 로직:**
```java
public List<ProductStatisticsEntity> execute(int limit) {
    List<ProductStatisticsEntity> allStatistics = productStatisticsRepository.findAll();

    return allStatistics.stream()
        .sorted(Comparator.comparingLong(ProductStatisticsEntity::getPopularityScore).reversed())
        .limit(limit)
        .toList();
}
```

### 2.2 현재 문제점

| 문제점 | 설명 |
|--------|------|
| ❌ 주문 연동 부재 | CreateOrderUseCase에서 판매 랭킹 업데이트 없음 |
| ❌ 취소 처리 부재 | CancelOrderUseCase에서 랭킹 차감 로직 없음 |
| ❌ TTL 미설정 | 고정 키(`daily:popular:products`) 사용으로 데이터 누적 |
| ❌ 날짜 기반 키 없음 | 오늘 날짜 기반의 동적 키 생성 없음 |
| ❌ 금주 랭킹 없음 | 금일 랭킹만 존재 (주간 랭킹 부재) |
| ❌ 페이징 미지원 | 전체 데이터 조회 후 애플리케이션에서 limit 처리 |
| ❌ 상품 정보 미조합 | productId만 반환 (상품명, 가격 등 정보 없음) |
| ❌ 동기 처리 | 주문 트랜잭션 내에서 랭킹 업데이트 시 성능 저하 우려 |

---

## 3. 개선 설계

### 3.1 Redis 키 설계

#### 3.1.1 새로운 키 구조

```
# 금일 판매 랭킹 (Sorted Set)
sales:ranking:daily:{yyyy-MM-dd}
- Score: 판매 수량 합계
- Value: productId
- TTL: 26시간

# 금주 판매 랭킹 (Sorted Set)
sales:ranking:weekly:{yyyy-ww}
- Score: 판매 수량 합계
- Value: productId
- TTL: 8일
```

#### 3.1.2 키 생성 예시

```java
// 2025-12-03 주문 시
"sales:ranking:daily:2025-12-03"     // 금일 랭킹
"sales:ranking:weekly:2025-49"       // 금주 랭킹 (49주차)
```

### 3.2 TTL 설정 근거

| 유형 | TTL | 근거 |
|------|-----|------|
| 금일 랭킹 | 26시간 | 자정 이후 2시간까지 전날 데이터 조회 가능 (시간대 고려) |
| 금주 랭킹 | 8일 | 7일 + 1일 (주간 데이터 보관 및 이월 시간 고려) |

### 3.3 아키텍처 설계

```
┌─────────────────────────────────────────────────────────────┐
│                     Order Layer                              │
├─────────────────────────────────────────────────────────────┤
│  CreateOrderUseCase          CancelOrderUseCase             │
│         │                            │                       │
│         │ (주문 완료 후)             │ (취소 완료 후)       │
│         ↓                            ↓                       │
│  [이벤트 발행]                 [이벤트 발행]                │
│    OrderCreatedEvent          OrderCancelledEvent           │
└─────────────────────────────────────────────────────────────┘
                       │                          │
                       ↓ (비동기)                 ↓ (비동기)
┌─────────────────────────────────────────────────────────────┐
│              Event Listener (@Async)                         │
├─────────────────────────────────────────────────────────────┤
│  OrderEventListener                                          │
│    - onOrderCreated()   → SalesRankingService.increase()    │
│    - onOrderCancelled() → SalesRankingService.decrease()    │
└─────────────────────────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────┐
│              Sales Ranking Service                           │
├─────────────────────────────────────────────────────────────┤
│  SalesRankingService                                         │
│    - increaseRanking(productId, quantity, orderDate)        │
│    - decreaseRanking(productId, quantity, orderDate)        │
│    - getDailySalesRanking(date, page, size)                 │
│    - getWeeklySalesRanking(weekYear, page, size)            │
└─────────────────────────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────┐
│              Redis Repository                                │
├─────────────────────────────────────────────────────────────┤
│  SalesRankingRepositoryImpl                                  │
│    - Redis Sorted Set Operations                            │
│    - TTL 관리                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 상세 구현 계획

### 4.1 도메인 이벤트 추가

#### 4.1.1 OrderCreatedEvent
```java
@Getter
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private List<OrderItemEntity> orderItems;
    private Long orderedAt;
}
```

#### 4.1.2 OrderCancelledEvent
```java
@Getter
@AllArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private List<OrderItemEntity> orderItems;
    private Long cancelledAt;
}
```

### 4.2 SalesRankingService

```java
@Service
@RequiredArgsConstructor
public class SalesRankingService {

    private final SalesRankingRepository salesRankingRepository;
    private final ProductRepository productRepository;

    /**
     * 판매 랭킹 증가 (주문 생성 시)
     */
    public void increaseRanking(Long productId, Integer quantity, Long orderDate) {
        LocalDate date = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(orderDate),
            ZoneId.of("Asia/Seoul")
        ).toLocalDate();

        // 금일 랭킹 업데이트
        String dailyKey = generateDailyKey(date);
        salesRankingRepository.incrementScore(dailyKey, productId, quantity);
        salesRankingRepository.setExpire(dailyKey, 26, TimeUnit.HOURS);

        // 금주 랭킹 업데이트
        String weeklyKey = generateWeeklyKey(date);
        salesRankingRepository.incrementScore(weeklyKey, productId, quantity);
        salesRankingRepository.setExpire(weeklyKey, 8, TimeUnit.DAYS);
    }

    /**
     * 판매 랭킹 감소 (주문 취소 시)
     */
    public void decreaseRanking(Long productId, Integer quantity, Long cancelDate) {
        LocalDate date = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(cancelDate),
            ZoneId.of("Asia/Seoul")
        ).toLocalDate();

        // 금일 랭킹 차감
        String dailyKey = generateDailyKey(date);
        salesRankingRepository.decrementScore(dailyKey, productId, quantity);

        // 금주 랭킹 차감
        String weeklyKey = generateWeeklyKey(date);
        salesRankingRepository.decrementScore(weeklyKey, productId, quantity);
    }

    /**
     * 금일 판매 랭킹 조회 (페이징 + 상품 정보 조합)
     */
    public SalesRankingResponse getDailySalesRanking(LocalDate date, int page, int size) {
        String dailyKey = generateDailyKey(date);

        long offset = (long) page * size;
        long end = offset + size - 1;

        // Redis에서 페이징 조회
        List<SalesRankingItem> rankings = salesRankingRepository
            .getRankingWithScores(dailyKey, offset, end);

        // 상품 정보 조합
        List<SalesRankingDto> result = rankings.stream()
            .map(item -> {
                ProductEntity product = productRepository.getOrThrow(item.getProductId());
                return new SalesRankingDto(
                    item.getRank(),
                    product.getId(),
                    product.getProductName(),
                    product.getPrice(),
                    item.getSalesCount()
                );
            })
            .toList();

        long totalCount = salesRankingRepository.getSize(dailyKey);

        return new SalesRankingResponse(
            "daily",
            date.toString(),
            result,
            page,
            size,
            totalCount
        );
    }

    /**
     * 금주 판매 랭킹 조회 (페이징 + 상품 정보 조합)
     */
    public SalesRankingResponse getWeeklySalesRanking(int year, int week, int page, int size) {
        String weeklyKey = "sales:ranking:weekly:" + year + "-" + (week < 10 ? "0" + week : week);

        long offset = (long) page * size;
        long end = offset + size - 1;

        // Redis에서 페이징 조회
        List<SalesRankingItem> rankings = salesRankingRepository
            .getRankingWithScores(weeklyKey, offset, end);

        // 상품 정보 조합
        List<SalesRankingDto> result = rankings.stream()
            .map(item -> {
                ProductEntity product = productRepository.getOrThrow(item.getProductId());
                return new SalesRankingDto(
                    item.getRank(),
                    product.getId(),
                    product.getProductName(),
                    product.getPrice(),
                    item.getSalesCount()
                );
            })
            .toList();

        long totalCount = salesRankingRepository.getSize(weeklyKey);

        return new SalesRankingResponse(
            "weekly",
            year + "-W" + (week < 10 ? "0" + week : week),
            result,
            page,
            size,
            totalCount
        );
    }

    private String generateDailyKey(LocalDate date) {
        return "sales:ranking:daily:" + date.toString();
    }

    private String generateWeeklyKey(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int year = date.get(weekFields.weekBasedYear());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        return "sales:ranking:weekly:" + year + "-" + (week < 10 ? "0" + week : week);
    }
}
```

### 4.3 SalesRankingRepository

```java
public interface SalesRankingRepository {
    void incrementScore(String key, Long productId, Integer quantity);
    void decrementScore(String key, Long productId, Integer quantity);
    void setExpire(String key, long timeout, TimeUnit timeUnit);
    List<SalesRankingItem> getRankingWithScores(String key, long start, long end);
    long getSize(String key);
}

@Repository
@RequiredArgsConstructor
public class SalesRankingRepositoryImpl implements SalesRankingRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void incrementScore(String key, Long productId, Integer quantity) {
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), quantity);
    }

    @Override
    public void decrementScore(String key, Long productId, Integer quantity) {
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), -quantity);
    }

    @Override
    public void setExpire(String key, long timeout, TimeUnit timeUnit) {
        redisTemplate.expire(key, timeout, timeUnit);
    }

    @Override
    public List<SalesRankingItem> getRankingWithScores(String key, long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> results =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        long rank = start + 1;
        List<SalesRankingItem> items = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> tuple : results) {
            items.add(new SalesRankingItem(
                rank++,
                Long.parseLong(tuple.getValue()),
                tuple.getScore().longValue()
            ));
        }

        return items;
    }

    @Override
    public long getSize(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0L;
    }
}
```

### 4.4 이벤트 리스너 (비동기 처리)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final SalesRankingService salesRankingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("주문 생성 이벤트 수신 - orderId: {}", event.getOrderId());

            for (OrderItemEntity item : event.getOrderItems()) {
                salesRankingService.increaseRanking(
                    item.getProductId(),
                    item.getQuantity(),
                    event.getOrderedAt()
                );
            }

            log.info("판매 랭킹 업데이트 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("판매 랭킹 업데이트 실패 - orderId: {}", event.getOrderId(), e);
            // 실패 시 재시도 로직 또는 별도 처리 (예: Dead Letter Queue)
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        try {
            log.info("주문 취소 이벤트 수신 - orderId: {}", event.getOrderId());

            for (OrderItemEntity item : event.getOrderItems()) {
                salesRankingService.decreaseRanking(
                    item.getProductId(),
                    item.getQuantity(),
                    event.getCancelledAt()
                );
            }

            log.info("판매 랭킹 차감 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("판매 랭킹 차감 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
}
```

### 4.5 UseCase 수정

#### 4.5.1 CreateOrderUseCase 수정
```java
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final ApplicationEventPublisher eventPublisher;
    // ... 기존 필드들

    public OrderEntity execute(CreateOrderRequest request) {
        // ... 기존 주문 생성 로직 (락 획득, 재고 차감 등)

        return transactionTemplate.execute(status -> {
            // ... 주문 저장 로직

            OrderEntity savedOrder = orderRepository.save(order);

            List<OrderItemEntity> savedItems = new ArrayList<>();
            for (OrderItemEntity orderItem : orderItems) {
                // ... 아이템 저장
                savedItems.add(savedItem);
            }

            // 이벤트 발행 (트랜잭션 커밋 후 비동기 실행)
            eventPublisher.publishEvent(new OrderCreatedEvent(
                savedOrder.getId(),
                savedItems,
                savedOrder.getOrderedAt()
            ));

            return savedOrder;
        });
    }
}
```

#### 4.5.2 CancelOrderUseCase 수정
```java
@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final OrderRepository orderRepository;

    public OrderEntity execute(long orderId) {
        OrderEntity order = orderRepository.getOrThrow(orderId);

        // ... 기존 취소 로직

        OrderEntity cancelledOrder = orderRepository.save(/* ... */);

        // 주문 아이템 조회
        List<OrderItemEntity> orderItems = orderRepository.getOrderItems(orderId);

        // 이벤트 발행 (트랜잭션 커밋 후 비동기 실행)
        eventPublisher.publishEvent(new OrderCancelledEvent(
            cancelledOrder.getId(),
            orderItems,
            cancelledOrder.getUpdatedAt()
        ));

        return cancelledOrder;
    }
}
```

### 4.6 Controller 및 API 응답

#### 4.6.1 SalesRankingController
```java
@RestController
@RequestMapping("/api/sales-rankings")
@RequiredArgsConstructor
public class SalesRankingController {

    private final SalesRankingService salesRankingService;

    /**
     * 금일 판매 랭킹 조회
     * GET /api/sales-rankings/daily?date=2025-12-03&page=0&size=10
     */
    @GetMapping("/daily")
    public ResponseEntity<SalesRankingResponse> getDailySalesRanking(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Seoul"));
        SalesRankingResponse response = salesRankingService.getDailySalesRanking(targetDate, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 금주 판매 랭킹 조회
     * GET /api/sales-rankings/weekly?year=2025&week=49&page=0&size=10
     */
    @GetMapping("/weekly")
    public ResponseEntity<SalesRankingResponse> getWeeklySalesRanking(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer week,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        int targetYear = (year != null) ? year : now.get(weekFields.weekBasedYear());
        int targetWeek = (week != null) ? week : now.get(weekFields.weekOfWeekBasedYear());

        SalesRankingResponse response = salesRankingService.getWeeklySalesRanking(
            targetYear, targetWeek, page, size
        );
        return ResponseEntity.ok(response);
    }
}
```

#### 4.6.2 응답 DTO

```java
@Getter
@AllArgsConstructor
public class SalesRankingResponse {
    private String type;              // "daily" or "weekly"
    private String period;            // "2025-12-03" or "2025-W49"
    private List<SalesRankingDto> rankings;
    private int page;
    private int size;
    private long totalCount;
}

@Getter
@AllArgsConstructor
public class SalesRankingDto {
    private long rank;               // 순위
    private Long productId;          // 상품 ID
    private String productName;      // 상품명
    private Long price;              // 가격
    private Long salesCount;         // 판매 수량
}

@Getter
@AllArgsConstructor
public class SalesRankingItem {
    private long rank;
    private Long productId;
    private Long salesCount;
}
```

### 4.7 비동기 설정

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sales-ranking-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

**설정 설명**

- **CorePoolSize(5)**: 기본 스레드 수로 일반적인 주문 처리량에 적합하며, 동시에 5개의 주문 이벤트를 비동기로 처리할 수 있습니다.

- **MaxPoolSize(10)**: 큐가 가득 찼을 때 생성할 수 있는 최대 스레드 수입니다. 특가 세일이나 플래시 세일 같은 순간적인 주문 폭주에 대응하기 위해 평소 5개에서 최대 10개까지 확장 가능하도록 설정했습니다.

- **QueueCapacity(100)**: 스레드가 모두 바쁠 때 대기할 수 있는 작업 개수입니다. 판매 랭킹 업데이트는 약간의 지연이 허용되므로 100개의 버퍼로 충분합니다. (최대 처리 용량: 스레드 10개 + 큐 100개 = 110개)

- **ThreadNamePrefix("sales-ranking-")**: 로그에서 `[sales-ranking-1]` 형태로 표시되어 디버깅 시 어떤 스레드가 작업했는지 쉽게 추적할 수 있습니다.

- **CallerRunsPolicy**: 스레드와 큐가 모두 가득 찬 극한 상황에서 작업을 거부하지 않고 호출한 메인 스레드에서 직접 실행합니다. 이를 통해 판매 랭킹 업데이트 누락을 방지하고 자연스러운 백프레셔를 제공합니다.

---

## 5. API 명세

### 5.1 금일 판매 랭킹 조회

```http
GET /api/sales-rankings/daily?date=2025-12-03&page=0&size=10
```

**Request Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| date | String (yyyy-MM-dd) | N | 오늘 | 조회할 날짜 |
| page | Integer | N | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | N | 10 | 페이지 크기 |

**Response (200 OK):**
```json
{
  "type": "daily",
  "period": "2025-12-03",
  "rankings": [
    {
      "rank": 1,
      "productId": 101,
      "productName": "무선 이어폰",
      "price": 129000,
      "salesCount": 523
    },
    {
      "rank": 2,
      "productId": 205,
      "productName": "블루투스 스피커",
      "price": 89000,
      "salesCount": 412
    }
  ],
  "page": 0,
  "size": 10,
  "totalCount": 156
}
```

### 5.2 금주 판매 랭킹 조회

```http
GET /api/sales-rankings/weekly?year=2025&week=49&page=0&size=20
```

**Request Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| year | Integer | N | 현재 연도 | 조회할 연도 |
| week | Integer | N | 현재 주차 | 조회할 주차 (1~53) |
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 10 | 페이지 크기 |

**Response (200 OK):**
```json
{
  "type": "weekly",
  "period": "2025-W49",
  "rankings": [
    {
      "rank": 1,
      "productId": 101,
      "productName": "무선 이어폰",
      "price": 129000,
      "salesCount": 3256
    },
    {
      "rank": 2,
      "productId": 205,
      "productName": "블루투스 스피커",
      "price": 89000,
      "salesCount": 2891
    }
  ],
  "page": 0,
  "size": 20,
  "totalCount": 342
}
```

---

## 6. 주요 변경 사항 요약

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| **Redis 키** | `daily:popular:products` (고정) | `sales:ranking:daily:{date}`, `sales:ranking:weekly:{year-week}` |
| **TTL** | 없음 (수동 삭제 필요) | 금일: 26시간, 금주: 8일 |
| **주문 연동** | 없음 | 주문 생성/취소 시 자동 업데이트 |
| **비동기 처리** | 없음 | Spring Event + @Async |
| **페이징** | 애플리케이션 레벨 (limit) | Redis ZRANGE 활용 (offset, limit) |
| **상품 정보** | productId만 반환 | 상품명, 가격 등 조합하여 반환 |
| **랭킹 종류** | 금일만 | 금일, 금주 |

---

## 7. 성능 및 안정성 고려사항

### 7.1 비동기 처리의 장점
1. **주문 처리 성능 향상**: 랭킹 업데이트가 주문 트랜잭션을 블로킹하지 않음
2. **장애 격리**: 랭킹 업데이트 실패가 주문 생성에 영향을 주지 않음
3. **확장성**: 이벤트 기반 아키텍처로 향후 다른 처리 추가 용이

### 7.2 데이터 정합성
- `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 사용
- 트랜잭션 커밋 후에만 이벤트 발행하여 일관성 보장
- 실패 시 로그 기록 및 모니터링 필요

### 7.3 Redis 성능
- Sorted Set의 ZINCRBY: O(log N) 시간 복잡도
- ZRANGE (페이징 조회): O(log N + M) (M = 조회 개수)
- 예상 처리량: 초당 10,000+ TPS (Redis 단일 인스턴스 기준)

### 7.4 TTL 자동 만료
- Redis의 자동 TTL 만료로 메모리 관리 용이
- 금일 랭킹: 다음날 오전 2시까지 유지
- 금주 랭킹: 다음 주 월요일까지 유지

---

## 8. 구현 우선순위

### Phase 1: 핵심 기능 (필수)
1. ✅ 도메인 이벤트 (OrderCreatedEvent, OrderCancelledEvent)
2. ✅ SalesRankingService (increase/decrease 로직)
3. ✅ SalesRankingRepository (Redis Sorted Set 연동)
4. ✅ 이벤트 리스너 (비동기 처리)
5. ✅ UseCase 수정 (이벤트 발행)

### Phase 2: API 및 조회 기능
6. ✅ 금일/금주 랭킹 조회 API
7. ✅ 페이징 처리
8. ✅ 상품 정보 조합

### Phase 3: 모니터링 및 최적화
9. ⏳ 로그 및 메트릭 수집
10. ⏳ 실패 재시도 메커니즘
11. ⏳ 캐싱 전략 (상품 정보)

---

## 9. 테스트 계획

### 9.1 단위 테스트
- SalesRankingService 로직 테스트
- SalesRankingRepository Redis 연동 테스트 (Embedded Redis)
- 이벤트 리스너 테스트

### 9.2 통합 테스트
- 주문 생성 → 랭킹 업데이트 플로우
- 주문 취소 → 랭킹 차감 플로우
- API 조회 (페이징, 상품 정보 조합)

### 9.3 성능 테스트
- 동시 주문 처리 시 랭킹 업데이트 성능
- 대용량 데이터 페이징 조회 성능
- Redis 메모리 사용량 모니터링

---

## 10. 결론

본 개선 작업을 통해 다음과 같은 효과를 기대할 수 있습니다:

1. **실시간성**: 주문 생성/취소 즉시 판매 랭킹 반영
2. **자동화**: 수동 배치 작업 없이 자동으로 랭킹 관리
3. **확장성**: 금일/금주 외에 월간, 카테고리별 랭킹 추가 용이
4. **성능**: 비동기 처리로 주문 처리 성능 영향 최소화
5. **사용자 경험**: 페이징 및 상품 정보 조합으로 즉시 사용 가능한 API 제공

**다음 단계**: Phase 1 핵심 기능부터 순차적으로 구현하여 안정성을 확보한 후, Phase 2, 3를 진행하는 것을 권장합니다.