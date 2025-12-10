package com.hhplus.ecommerce.application.salesRanking;

import com.hhplus.ecommerce.application.order.CancelOrderUseCase;
import com.hhplus.ecommerce.application.order.CreateOrderUseCase;
import com.hhplus.ecommerce.config.EmbeddedRedisConfig;
import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.product.jpa.ProductJpaRepository;
import com.hhplus.ecommerce.infrastructure.productOption.jpa.ProductOptionJpaRepository;
import com.hhplus.ecommerce.infrastructure.salesRanking.SalesRankingRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import com.hhplus.ecommerce.presentation.salesRanking.res.SalesRankingResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ContextConfiguration(initializers = EmbeddedRedisConfig.class)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SalesRankingIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;

    @Autowired
    private SalesRankingService salesRankingService;

    @Autowired
    private SalesRankingRepository salesRankingRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductOptionJpaRepository productOptionJpaRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private ProductEntity testProduct1;
    private ProductEntity testProduct2;
    private ProductOptionEntity testOption1;
    private ProductOptionEntity testOption2;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성
        long now = System.currentTimeMillis();
        testProduct1 = productJpaRepository.save(
            new ProductEntity(0L, 1L, "테스트상품1", null, 10000L, now, now)
        );
        testProduct2 = productJpaRepository.save(
            new ProductEntity(0L, 1L, "테스트상품2", null, 20000L, now, now)
        );

        testOption1 = productOptionJpaRepository.save(
            new ProductOptionEntity(0L, testProduct1.getId(), "옵션1", 0L, 1000L, now, now)
        );
        testOption2 = productOptionJpaRepository.save(
            new ProductOptionEntity(0L, testProduct2.getId(), "옵션2", 0L, 2000L, now, now)
        );
    }

    @AfterEach
    void cleanup() {
        // Redis 테스트 데이터 정리
        Set<String> keys = redisTemplate.keys("sales:ranking:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // DB 테스트 데이터 정리는 @Transactional이나 @DirtiesContext로 처리
        // 수동으로 delete 메서드가 없으므로 생략
    }

    @Test
    @DisplayName("주문 생성 시 판매 랭킹 증가 - 비동기 이벤트 처리")
    void createOrder_increasesRanking() throws InterruptedException {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
            1L,
            List.of(
                new OrderItemRequest(testOption1.getId(), 3),
                new OrderItemRequest(testOption2.getId(), 5)
            ),
            null
        );

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String dailyKey = "sales:ranking:daily:" + today.toString();

        // when
        OrderEntity order = createOrderUseCase.execute(request);

        // then
        // 비동기 처리를 기다림 (최대 5초)
        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Double score1 = redisTemplate.opsForZSet().score(dailyKey, testProduct1.getId().toString());
                Double score2 = redisTemplate.opsForZSet().score(dailyKey, testProduct2.getId().toString());

                assertThat(score1).isNotNull();
                assertThat(score1).isEqualTo(3.0);
                assertThat(score2).isNotNull();
                assertThat(score2).isEqualTo(5.0);
            });

        // 랭킹 조회 검증
        SalesRankingResponse response = salesRankingService.getDailySalesRanking(today, 0, 10);
        assertThat(response.getRankings()).hasSize(2);
        assertThat(response.getRankings().get(0).getProductId()).isEqualTo(testProduct2.getId());
        assertThat(response.getRankings().get(0).getSalesCount()).isEqualTo(5L);
        assertThat(response.getRankings().get(1).getProductId()).isEqualTo(testProduct1.getId());
        assertThat(response.getRankings().get(1).getSalesCount()).isEqualTo(3L);

        // 정리는 @Transactional 또는 테스트 프레임워크에 위임
    }

    @Test
    @DisplayName("주문 취소 시 판매 랭킹 감소 - 비동기 이벤트 처리")
    void cancelOrder_decreasesRanking() throws InterruptedException {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
            1L,
            List.of(
                new OrderItemRequest(testOption1.getId(), 10)
            ),
            null
        );

        OrderEntity order = createOrderUseCase.execute(request);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String dailyKey = "sales:ranking:daily:" + today.toString();

        // 주문 생성 후 랭킹 업데이트 대기
        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Double score = redisTemplate.opsForZSet().score(dailyKey, testProduct1.getId().toString());
                assertThat(score).isEqualTo(10.0);
            });

        // when - 주문 취소
        cancelOrderUseCase.execute(order.getId());

        // then - 랭킹 감소 확인 (비동기 처리 대기)
        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Double score = redisTemplate.opsForZSet().score(dailyKey, testProduct1.getId().toString());
                assertThat(score).isEqualTo(0.0);
            });

        // 정리는 @Transactional 또는 테스트 프레임워크에 위임
    }

    @Test
    @DisplayName("여러 주문 생성 후 랭킹 조회 - 정렬 순서 확인")
    void multipleOrders_rankingOrder() throws InterruptedException {
        // given - 3개의 주문 생성
        CreateOrderRequest request1 = new CreateOrderRequest(
            1L,
            List.of(new OrderItemRequest(testOption1.getId(), 5)),
            null
        );
        CreateOrderRequest request2 = new CreateOrderRequest(
            2L,
            List.of(new OrderItemRequest(testOption1.getId(), 3)),
            null
        );
        CreateOrderRequest request3 = new CreateOrderRequest(
            3L,
            List.of(new OrderItemRequest(testOption2.getId(), 10)),
            null
        );

        OrderEntity order1 = createOrderUseCase.execute(request1);
        OrderEntity order2 = createOrderUseCase.execute(request2);
        OrderEntity order3 = createOrderUseCase.execute(request3);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // when - 비동기 처리 완료 대기
        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
            .untilAsserted(() -> {
                SalesRankingResponse response = salesRankingService.getDailySalesRanking(today, 0, 10);
                assertThat(response.getRankings()).hasSize(2);
            });

        // then - 랭킹 검증
        SalesRankingResponse response = salesRankingService.getDailySalesRanking(today, 0, 10);
        assertThat(response.getRankings()).hasSize(2);

        // 첫 번째는 상품2 (10개)
        assertThat(response.getRankings().get(0).getProductId()).isEqualTo(testProduct2.getId());
        assertThat(response.getRankings().get(0).getSalesCount()).isEqualTo(10L);
        assertThat(response.getRankings().get(0).getRank()).isEqualTo(1L);

        // 두 번째는 상품1 (5 + 3 = 8개)
        assertThat(response.getRankings().get(1).getProductId()).isEqualTo(testProduct1.getId());
        assertThat(response.getRankings().get(1).getSalesCount()).isEqualTo(8L);
        assertThat(response.getRankings().get(1).getRank()).isEqualTo(2L);

        // 정리는 @Transactional 또는 테스트 프레임워크에 위임
    }
}