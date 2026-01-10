package com.hhplus.ecommerce.infrastructure.kafka.producer;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCancelledKafkaEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCreatedKafkaEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderEventType;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderKafkaEvent;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
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
    brokerProperties = {"listeners=PLAINTEXT://localhost:9096", "port=9096"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("OrderKafkaProducer 단위 테스트")
class OrderKafkaProducerTest {

    @Autowired
    private OrderKafkaProducer orderKafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, OrderKafkaEvent> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-order-group",
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
    @DisplayName("주문 생성 이벤트가 정상적으로 발행된다")
    void publishOrderCreatedEvent_Success() {
        // Given
        long now = System.currentTimeMillis();
        OrderEntity order = new OrderEntity(
            1L,
            100L, // userId
            50000,
            5000,
            45000,
            null,
            OrderStatus.PENDING,
            now,
            now,
            now
        );

        List<OrderItemEntity> orderItems = List.of(
            new OrderItemEntity(1L, 1L, 1L, 1L, "테스트 상품", "기본", 2, 30000, now),
            new OrderItemEntity(2L, 1L, 2L, 2L, "테스트 상품2", "대형", 1, 20000, now)
        );

        OrderCreatedKafkaEvent event = new OrderCreatedKafkaEvent(order, orderItems);

        // When
        orderKafkaProducer.publish(event);

        // Then
        ConsumerRecord<String, OrderKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.ORDER_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("1"); // orderId
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);
        assertThat(record.value().getOrderId()).isEqualTo(1L);

        OrderCreatedKafkaEvent receivedEvent = (OrderCreatedKafkaEvent) record.value();
        assertThat(receivedEvent.getUserId()).isEqualTo(100L);
        assertThat(receivedEvent.getTotalAmount()).isEqualTo(50000);
        assertThat(receivedEvent.getFinalAmount()).isEqualTo(45000);
        assertThat(receivedEvent.getOrderItems()).hasSize(2);
    }

    @Test
    @DisplayName("주문 취소 이벤트가 정상적으로 발행된다")
    void publishOrderCancelledEvent_Success() {
        // Given
        long now = System.currentTimeMillis();
        OrderEntity order = new OrderEntity(
            2L,
            200L, // userId
            30000,
            0,
            30000,
            null,
            OrderStatus.CANCELLED,
            now,
            now,
            now
        );

        List<OrderItemEntity> orderItems = List.of(
            new OrderItemEntity(3L, 2L, 3L, 3L, "취소된 상품", "소형", 1, 30000, now)
        );

        OrderCancelledKafkaEvent event = new OrderCancelledKafkaEvent(order, orderItems);

        // When
        orderKafkaProducer.publish(event);

        // Then
        ConsumerRecord<String, OrderKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.ORDER_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("2"); // orderId
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getEventType()).isEqualTo(OrderEventType.ORDER_CANCELLED);
        assertThat(record.value().getOrderId()).isEqualTo(2L);

        OrderCancelledKafkaEvent receivedEvent = (OrderCancelledKafkaEvent) record.value();
        assertThat(receivedEvent.getUserId()).isEqualTo(200L);
        assertThat(receivedEvent.getOrderItems()).hasSize(1);
    }

    @Test
    @DisplayName("여러 주문 생성 이벤트가 순서대로 발행된다")
    void publishMultipleOrderCreatedEvents_InOrder() {
        // Given & When - 동일 주문(orderId=3)의 여러 이벤트 발행
        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            OrderEntity order = new OrderEntity(
                3L, // 동일 orderId
                300L,
                10000,
                0,
                10000,
                null,
                OrderStatus.PENDING,
                now + i,
                now + i,
                now + i
            );

            List<OrderItemEntity> orderItems = List.of(
                new OrderItemEntity((long) i, 3L, (long) i, (long) i, "상품" + i, "옵션", 1, 10000, now)
            );

            OrderCreatedKafkaEvent event = new OrderCreatedKafkaEvent(order, orderItems);
            orderKafkaProducer.publish(event);
        }

        // Then - 3개의 이벤트가 모두 발행되었는지 확인
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        int count = 0;
        for (ConsumerRecord<String, OrderKafkaEvent> record : records) {
            if (record.topic().equals(KafkaTopics.ORDER_EVENTS)) {
                assertThat(record.key()).isEqualTo("3"); // 동일 orderId
                assertThat(record.value().getOrderId()).isEqualTo(3L);
                assertThat(record.value().getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);
                count++;
            }
        }
        assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("여러 주문 취소 이벤트가 순서대로 발행된다")
    void publishMultipleOrderCancelledEvents_InOrder() {
        // Given & When - 여러 주문의 취소 이벤트 발행
        long now = System.currentTimeMillis();
        for (long orderId = 10; orderId < 13; orderId++) {
            OrderEntity order = new OrderEntity(
                orderId,
                400L,
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
                new OrderItemEntity(orderId * 10, orderId, orderId, orderId, "취소상품", "옵션", 1, 15000, now)
            );

            OrderCancelledKafkaEvent event = new OrderCancelledKafkaEvent(order, orderItems);
            orderKafkaProducer.publish(event);
        }

        // Then - 3개의 이벤트가 모두 발행되었는지 확인
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        int count = 0;
        for (ConsumerRecord<String, OrderKafkaEvent> record : records) {
            if (record.topic().equals(KafkaTopics.ORDER_EVENTS)) {
                assertThat(record.value().getEventType()).isEqualTo(OrderEventType.ORDER_CANCELLED);
                count++;
            }
        }
        assertThat(count).isGreaterThanOrEqualTo(3);
    }
}
