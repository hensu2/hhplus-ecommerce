package com.hhplus.ecommerce.infrastructure.kafka.producer;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockEventType;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockKafkaEvent;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaTopics.STOCK_EVENTS},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9099", "port=9099"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("StockKafkaProducer 단위 테스트")
class StockKafkaProducerTest {

    @Autowired
    private StockKafkaProducer stockKafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, StockKafkaEvent> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-stock-group",
            "true",
            embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, StockKafkaEvent.class.getName());

        ConsumerFactory<String, StockKafkaEvent> consumerFactory =
            new DefaultKafkaConsumerFactory<>(consumerProps);

        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaTopics.STOCK_EVENTS));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("재고 증가 이벤트가 정상적으로 발행된다")
    void publishStockIncreasedEvent_Success() {
        // Given
        long now = System.currentTimeMillis();
        ProductOptionEntity option = new ProductOptionEntity(
            1L, 100L, "기본 옵션", 0L, 50L, now, now
        );

        StockChangedKafkaEvent event = new StockChangedKafkaEvent(
            StockEventType.STOCK_INCREASED,
            option,
            "테스트 상품",
            50L,
            100L,
            50,
            "ADMIN_UPDATE"
        );

        // When
        stockKafkaProducer.publish(event);

        // Then
        ConsumerRecord<String, StockKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.STOCK_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("1"); // productOptionId
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getEventType()).isEqualTo(StockEventType.STOCK_INCREASED);

        StockChangedKafkaEvent receivedEvent = (StockChangedKafkaEvent) record.value();
        assertThat(receivedEvent.getProductId()).isEqualTo(100L);
        assertThat(receivedEvent.getProductName()).isEqualTo("테스트 상품");
        assertThat(receivedEvent.getPreviousStock()).isEqualTo(50L);
        assertThat(receivedEvent.getCurrentStock()).isEqualTo(100L);
        assertThat(receivedEvent.getChangeAmount()).isEqualTo(50);
    }

    @Test
    @DisplayName("재고 감소 이벤트가 정상적으로 발행된다")
    void publishStockDecreasedEvent_Success() {
        // Given
        long now = System.currentTimeMillis();
        ProductOptionEntity option = new ProductOptionEntity(
            2L, 200L, "대형 옵션", 5000L, 30L, now, now
        );

        StockChangedKafkaEvent event = new StockChangedKafkaEvent(
            StockEventType.STOCK_DECREASED,
            option,
            "테스트 상품2",
            50L,
            30L,
            20,
            "ORDER_CREATED"
        );

        // When
        stockKafkaProducer.publish(event);

        // Then
        ConsumerRecord<String, StockKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.STOCK_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("2");
        assertThat(record.value().getEventType()).isEqualTo(StockEventType.STOCK_DECREASED);

        StockChangedKafkaEvent receivedEvent = (StockChangedKafkaEvent) record.value();
        assertThat(receivedEvent.getPreviousStock()).isEqualTo(50L);
        assertThat(receivedEvent.getCurrentStock()).isEqualTo(30L);
        assertThat(receivedEvent.getChangeReason()).isEqualTo("ORDER_CREATED");
    }

    @Test
    @DisplayName("여러 재고 증가 이벤트가 순서대로 발행된다")
    void publishMultipleStockIncreasedEvents_InOrder() {
        // Given & When - 동일 상품 옵션(id=3)의 여러 이벤트 발행
        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            ProductOptionEntity option = new ProductOptionEntity(
                3L, 300L, "소형 옵션", 0L, (long) (10 + i * 10), now, now
            );

            StockChangedKafkaEvent event = new StockChangedKafkaEvent(
                StockEventType.STOCK_INCREASED,
                option,
                "상품" + i,
                (long) (i * 10),
                (long) (10 + i * 10),
                10,
                "ADMIN_UPDATE"
            );
            stockKafkaProducer.publish(event);
        }

        // Then - 3개의 이벤트가 모두 발행되었는지 확인
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        int count = 0;
        for (ConsumerRecord<String, StockKafkaEvent> record : records) {
            if (record.topic().equals(KafkaTopics.STOCK_EVENTS)) {
                assertThat(record.key()).isEqualTo("3"); // 동일 productOptionId
                assertThat(record.value().getProductOptionId()).isEqualTo(3L);
                assertThat(record.value().getEventType()).isEqualTo(StockEventType.STOCK_INCREASED);
                count++;
            }
        }
        assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("여러 재고 감소 이벤트가 순서대로 발행된다")
    void publishMultipleStockDecreasedEvents_InOrder() {
        // Given & When - 여러 상품 옵션의 감소 이벤트 발행
        long now = System.currentTimeMillis();
        for (long optionId = 10; optionId < 13; optionId++) {
            ProductOptionEntity option = new ProductOptionEntity(
                optionId, 400L, "중형 옵션", 3000L, 20L, now, now
            );

            StockChangedKafkaEvent event = new StockChangedKafkaEvent(
                StockEventType.STOCK_DECREASED,
                option,
                "감소 상품",
                50L,
                20L,
                30,
                "ORDER_CREATED"
            );
            stockKafkaProducer.publish(event);
        }

        // Then - 3개의 이벤트가 모두 발행되었는지 확인
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        int count = 0;
        for (ConsumerRecord<String, StockKafkaEvent> record : records) {
            if (record.topic().equals(KafkaTopics.STOCK_EVENTS)) {
                assertThat(record.value().getEventType()).isEqualTo(StockEventType.STOCK_DECREASED);
                count++;
            }
        }
        assertThat(count).isGreaterThanOrEqualTo(3);
    }
}
