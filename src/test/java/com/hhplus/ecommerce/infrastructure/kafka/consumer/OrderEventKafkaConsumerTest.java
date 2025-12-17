package com.hhplus.ecommerce.infrastructure.kafka.consumer;

import com.hhplus.ecommerce.application.salesRanking.SalesRankingService;
import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCancelledKafkaEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCreatedKafkaEvent;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import com.hhplus.ecommerce.infrastructure.kafka.producer.OrderKafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaTopics.ORDER_EVENTS},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9097", "port=9097"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("OrderEventKafkaConsumer 단위 테스트")
class OrderEventKafkaConsumerTest {

    @Autowired
    private OrderKafkaProducer orderKafkaProducer;

    @SpyBean
    private SalesRankingService salesRankingService;

    @Test
    @DisplayName("주문 생성 이벤트 수신 시 판매 랭킹이 증가한다")
    void consumeOrderCreatedEvent_IncreasesRanking() {
        // Given
        long now = System.currentTimeMillis();
        OrderEntity order = new OrderEntity(
            100L,
            1L,
            60000,
            0,
            60000,
            null,
            OrderStatus.PENDING,
            now,
            now,
            now
        );

        List<OrderItemEntity> orderItems = List.of(
            new OrderItemEntity(1L, 100L, 10L, 10L, "상품A", "기본", 2, 30000, now),
            new OrderItemEntity(2L, 100L, 20L, 20L, "상품B", "대형", 3, 30000, now)
        );

        OrderCreatedKafkaEvent event = new OrderCreatedKafkaEvent(order, orderItems);

        // When
        orderKafkaProducer.publish(event);

        // Then - SalesRankingService.increaseRanking이 호출되었는지 확인
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(salesRankingService, atLeastOnce()).increaseRanking(
                eq(10L),
                eq(2),
                anyLong()
            );
            verify(salesRankingService, atLeastOnce()).increaseRanking(
                eq(20L),
                eq(3),
                anyLong()
            );
        });
    }

    @Test
    @DisplayName("주문 취소 이벤트 수신 시 판매 랭킹이 감소한다")
    void consumeOrderCancelledEvent_DecreasesRanking() {
        // Given
        long now = System.currentTimeMillis();
        OrderEntity order = new OrderEntity(
            200L,
            2L,
            40000,
            0,
            40000,
            null,
            OrderStatus.CANCELLED,
            now,
            now,
            now
        );

        List<OrderItemEntity> orderItems = List.of(
            new OrderItemEntity(3L, 200L, 30L, 30L, "상품C", "소형", 1, 20000, now),
            new OrderItemEntity(4L, 200L, 40L, 40L, "상품D", "중형", 2, 20000, now)
        );

        OrderCancelledKafkaEvent event = new OrderCancelledKafkaEvent(order, orderItems);

        // When
        orderKafkaProducer.publish(event);

        // Then - SalesRankingService.decreaseRanking이 호출되었는지 확인
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(salesRankingService, atLeastOnce()).decreaseRanking(
                eq(30L),
                eq(1),
                anyLong()
            );
            verify(salesRankingService, atLeastOnce()).decreaseRanking(
                eq(40L),
                eq(2),
                anyLong()
            );
        });
    }

    @Test
    @DisplayName("여러 주문 생성 이벤트 수신 시 모든 판매 랭킹이 증가한다")
    void consumeMultipleOrderCreatedEvents_IncreasesAllRankings() {
        // Given & When - 여러 주문 생성 이벤트 발행
        long now = System.currentTimeMillis();
        for (long orderId = 300; orderId < 303; orderId++) {
            OrderEntity order = new OrderEntity(
                orderId,
                3L,
                10000,
                0,
                10000,
                null,
                OrderStatus.PENDING,
                now,
                now,
                now
            );

            List<OrderItemEntity> orderItems = List.of(
                new OrderItemEntity(orderId * 10, orderId, 50L, 50L, "상품E", "기본", 1, 10000, now)
            );

            OrderCreatedKafkaEvent event = new OrderCreatedKafkaEvent(order, orderItems);
            orderKafkaProducer.publish(event);
        }

        // Then - 3번 호출되었는지 확인
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(salesRankingService, times(3)).increaseRanking(
                eq(50L),
                eq(1),
                anyLong()
            );
        });
    }

    @Test
    @DisplayName("여러 주문 취소 이벤트 수신 시 모든 판매 랭킹이 감소한다")
    void consumeMultipleOrderCancelledEvents_DecreasesAllRankings() {
        // Given & When - 여러 주문 취소 이벤트 발행
        long now = System.currentTimeMillis();
        for (long orderId = 400; orderId < 403; orderId++) {
            OrderEntity order = new OrderEntity(
                orderId,
                4L,
                15000,
                0,
                15000,
                null,
                OrderStatus.CANCELLED,
                now,
                now,
                now
            );

            List<OrderItemEntity> orderItems = List.of(
                new OrderItemEntity(orderId * 10, orderId, 60L, 60L, "상품F", "기본", 2, 15000, now)
            );

            OrderCancelledKafkaEvent event = new OrderCancelledKafkaEvent(order, orderItems);
            orderKafkaProducer.publish(event);
        }

        // Then - 3번 호출되었는지 확인
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(salesRankingService, times(3)).decreaseRanking(
                eq(60L),
                eq(2),
                anyLong()
            );
        });
    }
}
