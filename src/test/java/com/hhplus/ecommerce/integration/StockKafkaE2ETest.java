package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.productOption.DecreaseStockUseCase;
import com.hhplus.ecommerce.application.productOption.UpdateStockUseCase;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockEventType;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockKafkaEvent;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import com.hhplus.ecommerce.infrastructure.product.jpa.ProductJpaRepository;
import com.hhplus.ecommerce.infrastructure.productOption.jpa.ProductOptionJpaRepository;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
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
    brokerProperties = {"listeners=PLAINTEXT://localhost:9100", "port=9100"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("재고 Kafka E2E 테스트")
class StockKafkaE2ETest {

    @Autowired
    private UpdateStockUseCase updateStockUseCase;

    @Autowired
    private DecreaseStockUseCase decreaseStockUseCase;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductOptionJpaRepository productOptionJpaRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, StockKafkaEvent> consumer;

    @BeforeEach
    void setUp() {
        // Kafka Consumer 설정
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-stock-e2e-group",
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
    @DisplayName("재고 증가 전체 플로우: 재고 증가 → DB 저장 → Kafka 이벤트 발행")
    void increaseStockFullFlow_WithKafkaEvent() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null, 1L, "재고 테스트 상품", "테스트 설명", 10000L, now, now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null, savedProduct.getId(), "기본 옵션", 0L, 50L, now, now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        // When - 재고 증가
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.INCREASE, 50);
        ProductOptionEntity updatedOption = updateStockUseCase.execute(savedOption.getId(), request);

        // Then - DB 저장 확인
        assertThat(updatedOption).isNotNull();
        assertThat(updatedOption.getStock()).isEqualTo(100L);

        // Then - Kafka 이벤트 확인
        ConsumerRecord<String, StockKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.STOCK_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(savedOption.getId()));
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getEventType()).isEqualTo(StockEventType.STOCK_INCREASED);

        StockChangedKafkaEvent event = (StockChangedKafkaEvent) record.value();
        assertThat(event.getProductId()).isEqualTo(savedProduct.getId());
        assertThat(event.getProductName()).isEqualTo("재고 테스트 상품");
        assertThat(event.getPreviousStock()).isEqualTo(50L);
        assertThat(event.getCurrentStock()).isEqualTo(100L);
        assertThat(event.getChangeAmount()).isEqualTo(50);
        assertThat(event.getChangeReason()).isEqualTo("ADMIN_UPDATE");
    }

    @Test
    @DisplayName("재고 감소 전체 플로우: 재고 감소 → DB 저장 → Kafka 이벤트 발행")
    void decreaseStockFullFlow_WithKafkaEvent() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null, 1L, "재고 감소 테스트 상품", "감소 테스트", 15000L, now, now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null, savedProduct.getId(), "대형 옵션", 5000L, 100L, now, now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        // When - 재고 감소
        ProductOptionEntity updatedOption = decreaseStockUseCase.execute(savedOption.getId(), 30L);

        // Then - DB 저장 확인
        assertThat(updatedOption).isNotNull();
        assertThat(updatedOption.getStock()).isEqualTo(70L);

        // Then - Kafka 이벤트 확인
        ConsumerRecord<String, StockKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.STOCK_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(savedOption.getId()));
        assertThat(record.value().getEventType()).isEqualTo(StockEventType.STOCK_DECREASED);

        StockChangedKafkaEvent event = (StockChangedKafkaEvent) record.value();
        assertThat(event.getProductName()).isEqualTo("재고 감소 테스트 상품");
        assertThat(event.getPreviousStock()).isEqualTo(100L);
        assertThat(event.getCurrentStock()).isEqualTo(70L);
        assertThat(event.getChangeAmount()).isEqualTo(30);
        assertThat(event.getChangeReason()).isEqualTo("DIRECT_DECREASE");
    }

    @Test
    @DisplayName("여러 재고 변경 시 모두 Kafka 이벤트로 발행된다")
    void multipleStockChanges_AllPublishedToKafka() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null, 1L, "다량 변경 테스트 상품", "다량 변경", 5000L, now, now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null, savedProduct.getId(), "일반 옵션", 0L, 100L, now, now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        // When - 여러 재고 변경 (증가 → 감소 → 증가)
        updateStockUseCase.execute(savedOption.getId(), new UpdateStockRequest(StockUpdateType.INCREASE, 50));
        updateStockUseCase.execute(savedOption.getId(), new UpdateStockRequest(StockUpdateType.DECREASE, 30));
        updateStockUseCase.execute(savedOption.getId(), new UpdateStockRequest(StockUpdateType.INCREASE, 20));

        // Then - 3개의 이벤트 확인
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        int increasedCount = 0;
        int decreasedCount = 0;
        for (ConsumerRecord<String, StockKafkaEvent> record : records) {
            if (record.topic().equals(KafkaTopics.STOCK_EVENTS) &&
                record.value().getProductOptionId().equals(savedOption.getId())) {

                if (record.value().getEventType() == StockEventType.STOCK_INCREASED) {
                    increasedCount++;
                } else if (record.value().getEventType() == StockEventType.STOCK_DECREASED) {
                    decreasedCount++;
                }
            }
        }

        assertThat(increasedCount).isEqualTo(2);
        assertThat(decreasedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("재고 설정 전체 플로우: 재고 설정 → DB 저장 → Kafka 이벤트 발행")
    void setStockFullFlow_WithKafkaEvent() {
        // Given - 상품 및 옵션 생성
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(
            null, 1L, "재고 설정 테스트 상품", "설정 테스트", 20000L, now, now
        );
        ProductEntity savedProduct = productJpaRepository.save(product);

        ProductOptionEntity option = new ProductOptionEntity(
            null, savedProduct.getId(), "설정 옵션", 0L, 50L, now, now
        );
        ProductOptionEntity savedOption = productOptionJpaRepository.save(option);

        // When - 재고 설정
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 200);
        ProductOptionEntity updatedOption = updateStockUseCase.execute(savedOption.getId(), request);

        // Then - DB 저장 확인
        assertThat(updatedOption).isNotNull();
        assertThat(updatedOption.getStock()).isEqualTo(200L);

        // Then - Kafka 이벤트 확인
        ConsumerRecord<String, StockKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.STOCK_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.value().getEventType()).isEqualTo(StockEventType.STOCK_UPDATED);

        StockChangedKafkaEvent event = (StockChangedKafkaEvent) record.value();
        assertThat(event.getPreviousStock()).isEqualTo(50L);
        assertThat(event.getCurrentStock()).isEqualTo(200L);
        assertThat(event.getChangeAmount()).isEqualTo(200);
    }
}
