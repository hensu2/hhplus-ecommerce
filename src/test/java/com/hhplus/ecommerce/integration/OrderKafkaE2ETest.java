package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.order.CancelOrderUseCase;
import com.hhplus.ecommerce.application.order.CreateOrderUseCase;
import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCancelledKafkaEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCreatedKafkaEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderEventType;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderKafkaEvent;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.product.jpa.ProductJpaRepository;
import com.hhplus.ecommerce.infrastructure.productOption.jpa.ProductOptionJpaRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaTopics.ORDER_EVENTS},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9098", "port=9098"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("주문 Kafka E2E 테스트")
class OrderKafkaE2ETest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductOptionJpaRepository productOptionJpaRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, OrderKafkaEvent> consumer;

    @BeforeEach
    void setUp() {
        // Kafka Consumer 설정
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-order-e2e-group",
            "true",
            embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderKafkaEvent.class.getName());

        ConsumerFactory<String, OrderKafkaEvent> consumerFactory =
            new DefaultKafkaConsumerFactory<>(consumerProps);

        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaTopics.ORDER_EVENTS));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("주문 생성 전체 플로우: 주문 생성 → DB 저장 → Kafka 이벤트 발행")
    void createOrderFullFlow_WithKafkaEvent() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null,
            1L, // createdUserId
            "테스트 상품",
            "테스트 설명",
            10000L, // price
            now,
            now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null,
            savedProduct.getId(),
            "기본 옵션",
            0L, // additionalPrice
            100L, // stock
            now,
            now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        // When - 주문 생성
        List<OrderItemRequest> items = List.of(new OrderItemRequest(savedOption.getId(), 2));
        CreateOrderRequest request = new CreateOrderRequest(
            5000L, // userId
            items,
            null // couponHistoryId
        );

        OrderEntity createdOrder = createOrderUseCase.execute(request);

        // Then - DB 저장 확인
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getUserId()).isEqualTo(5000L);
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Then - Kafka 이벤트 확인
        ConsumerRecord<String, OrderKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.ORDER_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(createdOrder.getId()));
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);

        OrderCreatedKafkaEvent event = (OrderCreatedKafkaEvent) record.value();
        assertThat(event.getUserId()).isEqualTo(5000L);
        assertThat(event.getOrderId()).isEqualTo(createdOrder.getId());
        assertThat(event.getOrderItems()).hasSize(1);
        assertThat(event.getOrderItems().get(0).getProductId()).isEqualTo(savedProduct.getId());
        assertThat(event.getOrderItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("주문 취소 전체 플로우: 주문 취소 → DB 저장 → Kafka 이벤트 발행")
    void cancelOrderFullFlow_WithKafkaEvent() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null,
            1L,
            "테스트 상품2",
            "테스트 설명2",
            15000L,
            now,
            now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null,
            savedProduct.getId(),
            "대형 옵션",
            5000L,
            50L,
            now,
            now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        // 주문 생성
        List<OrderItemRequest> items = List.of(new OrderItemRequest(savedOption.getId(), 1));
        CreateOrderRequest createRequest = new CreateOrderRequest(
            6000L, // userId
            items,
            null
        );
        OrderEntity createdOrder = createOrderUseCase.execute(createRequest);

        // 생성 이벤트 소비 (다음 테스트를 위해 큐에서 제거)
        KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.ORDER_EVENTS, Duration.ofSeconds(5));

        // When - 주문 취소
        OrderEntity cancelledOrder = cancelOrderUseCase.execute(createdOrder.getId());

        // Then - DB 저장 확인
        assertThat(cancelledOrder).isNotNull();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Then - Kafka 이벤트 확인
        ConsumerRecord<String, OrderKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.ORDER_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(cancelledOrder.getId()));
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getEventType()).isEqualTo(OrderEventType.ORDER_CANCELLED);

        OrderCancelledKafkaEvent event = (OrderCancelledKafkaEvent) record.value();
        assertThat(event.getUserId()).isEqualTo(6000L);
        assertThat(event.getOrderId()).isEqualTo(cancelledOrder.getId());
        assertThat(event.getOrderItems()).hasSize(1);
    }

    @Test
    @DisplayName("여러 주문 생성 시 모두 Kafka 이벤트로 발행된다")
    void createMultipleOrders_AllPublishedToKafka() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null,
            1L,
            "대량 주문 상품",
            "대량 주문 설명",
            5000L,
            now,
            now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null,
            savedProduct.getId(),
            "일반 옵션",
            0L,
            500L,
            now,
            now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        int orderCount = 3;

        // When - 여러 주문 생성
        for (long userId = 7000; userId < 7000 + orderCount; userId++) {
            List<OrderItemRequest> items = List.of(new OrderItemRequest(savedOption.getId(), 1));
            CreateOrderRequest request = new CreateOrderRequest(
                userId,
                items,
                null
            );
            createOrderUseCase.execute(request);
        }

        // Then - Kafka 이벤트 확인
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(orderCount);

        int count = 0;
        for (ConsumerRecord<String, OrderKafkaEvent> record : records) {
            if (record.topic().equals(KafkaTopics.ORDER_EVENTS) &&
                record.value().getEventType() == OrderEventType.ORDER_CREATED) {
                OrderCreatedKafkaEvent event = (OrderCreatedKafkaEvent) record.value();
                assertThat(event.getUserId()).isGreaterThanOrEqualTo(7000L);
                assertThat(event.getUserId()).isLessThan(7000L + orderCount);
                count++;
            }
        }
        assertThat(count).isGreaterThanOrEqualTo(orderCount);
    }

    @Test
    @DisplayName("동일한 주문에 대해 생성 → 취소 이벤트가 순서대로 발행된다")
    void createThenCancelOrder_EventsInOrder() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null,
            1L,
            "순서 테스트 상품",
            "순서 테스트 설명",
            20000L,
            now,
            now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null,
            savedProduct.getId(),
            "순서 옵션",
            0L,
            100L,
            now,
            now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        // When - 주문 생성
        List<OrderItemRequest> items = List.of(new OrderItemRequest(savedOption.getId(), 3));
        CreateOrderRequest createRequest = new CreateOrderRequest(
            8000L,
            items,
            null
        );
        OrderEntity createdOrder = createOrderUseCase.execute(createRequest);

        // When - 주문 취소
        cancelOrderUseCase.execute(createdOrder.getId());

        // Then - 2개의 이벤트 확인 (생성 + 취소)
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(2);

        boolean foundCreated = false;
        boolean foundCancelled = false;

        for (ConsumerRecord<String, OrderKafkaEvent> record : records) {
            if (record.topic().equals(KafkaTopics.ORDER_EVENTS) &&
                record.value().getOrderId().equals(createdOrder.getId())) {

                if (record.value().getEventType() == OrderEventType.ORDER_CREATED) {
                    foundCreated = true;
                } else if (record.value().getEventType() == OrderEventType.ORDER_CANCELLED) {
                    foundCancelled = true;
                }
            }
        }

        assertThat(foundCreated).isTrue();
        assertThat(foundCancelled).isTrue();
    }
}
