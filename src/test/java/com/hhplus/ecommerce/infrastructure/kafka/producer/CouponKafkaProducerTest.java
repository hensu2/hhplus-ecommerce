package com.hhplus.ecommerce.infrastructure.kafka.producer;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.domain.coupon.event.kafka.CouponIssuedKafkaEvent;
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
    topics = {KafkaTopics.COUPON_EVENTS},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("CouponKafkaProducer 단위 테스트")
class CouponKafkaProducerTest {

    @Autowired
    private CouponKafkaProducer couponKafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, CouponIssuedKafkaEvent> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-group",
            "true",
            embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CouponIssuedKafkaEvent.class.getName());

        ConsumerFactory<String, CouponIssuedKafkaEvent> consumerFactory =
            new DefaultKafkaConsumerFactory<>(consumerProps);

        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaTopics.COUPON_EVENTS));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("PERCENT 타입 쿠폰 발급 이벤트가 정상적으로 발행된다")
    void publishPercentCouponIssuedEvent_Success() {
        // Given
        CouponEntity coupon = new CouponEntity(
            1L,
            "퍼센트 할인 쿠폰",
            DiscountType.PERCENT,
            10,
            10000,
            100000,
            100,
            System.currentTimeMillis(),
            System.currentTimeMillis() + 86400000L,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        CouponHistoryEntity history = new CouponHistoryEntity(
            100L,
            1L,
            1L,
            CouponStatus.ISSUED,
            System.currentTimeMillis(),
            null
        );

        CouponIssuedKafkaEvent event = new CouponIssuedKafkaEvent(history, coupon);

        // When
        couponKafkaProducer.publish(event);

        // Then
        ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.COUPON_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("1"); // userId
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getCouponHistoryId()).isEqualTo(100L);
        assertThat(record.value().getCouponId()).isEqualTo(1L);
        assertThat(record.value().getCouponName()).isEqualTo("퍼센트 할인 쿠폰");
        assertThat(record.value().getUserId()).isEqualTo(1L);
        assertThat(record.value().getDiscountType()).isEqualTo("PERCENT");
        assertThat(record.value().getDiscountAmount()).isEqualTo(10);
    }

    @Test
    @DisplayName("AMOUNT 타입 쿠폰 발급 이벤트가 정상적으로 발행된다")
    void publishAmountCouponIssuedEvent_Success() {
        // Given
        CouponEntity coupon = new CouponEntity(
            2L,
            "정액 할인 쿠폰",
            DiscountType.AMOUNT,
            5000,
            10000,
            100000,
            100,
            System.currentTimeMillis(),
            System.currentTimeMillis() + 86400000L,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        CouponHistoryEntity history = new CouponHistoryEntity(
            200L,
            2L,
            2L,
            CouponStatus.ISSUED,
            System.currentTimeMillis(),
            null
        );

        CouponIssuedKafkaEvent event = new CouponIssuedKafkaEvent(history, coupon);

        // When
        couponKafkaProducer.publish(event);

        // Then
        ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.COUPON_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("2"); // userId
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getCouponHistoryId()).isEqualTo(200L);
        assertThat(record.value().getCouponId()).isEqualTo(2L);
        assertThat(record.value().getCouponName()).isEqualTo("정액 할인 쿠폰");
        assertThat(record.value().getUserId()).isEqualTo(2L);
        assertThat(record.value().getDiscountType()).isEqualTo("AMOUNT");
        assertThat(record.value().getDiscountAmount()).isEqualTo(5000);
    }

    @Test
    @DisplayName("PERCENT 타입 - 여러 쿠폰 발급 이벤트가 순서대로 발행된다")
    void publishMultiplePercentCouponEvents_InOrder() {
        // Given
        CouponEntity coupon = new CouponEntity(
            1L,
            "10% 할인 쿠폰",
            DiscountType.PERCENT,
            10,
            10000,
            100000,
            100,
            System.currentTimeMillis(),
            System.currentTimeMillis() + 86400000L,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        // When - 동일 사용자(userId=1)의 여러 이벤트 발행
        for (long i = 1; i <= 3; i++) {
            CouponHistoryEntity history = new CouponHistoryEntity(
                100L + i,
                1L, // 동일 userId
                1L,
                CouponStatus.ISSUED,
                System.currentTimeMillis(),
                null
            );
            CouponIssuedKafkaEvent event = new CouponIssuedKafkaEvent(history, coupon);
            couponKafkaProducer.publish(event);
        }

        // Then - 3개의 이벤트가 모두 발행되었는지 확인
        for (int i = 0; i < 3; i++) {
            ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                KafkaTopics.COUPON_EVENTS,
                Duration.ofSeconds(10)
            );

            assertThat(record).isNotNull();
            assertThat(record.key()).isEqualTo("1"); // 동일 userId
            assertThat(record.value().getUserId()).isEqualTo(1L);
            assertThat(record.value().getDiscountType()).isEqualTo("PERCENT");
        }
    }

    @Test
    @DisplayName("AMOUNT 타입 - 여러 쿠폰 발급 이벤트가 순서대로 발행된다")
    void publishMultipleAmountCouponEvents_InOrder() {
        // Given
        CouponEntity coupon = new CouponEntity(
            3L,
            "3000원 할인 쿠폰",
            DiscountType.AMOUNT,
            3000,
            10000,
            100000,
            100,
            System.currentTimeMillis(),
            System.currentTimeMillis() + 86400000L,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        // When - 동일 사용자(userId=3)의 여러 이벤트 발행
        for (long i = 1; i <= 3; i++) {
            CouponHistoryEntity history = new CouponHistoryEntity(
                300L + i,
                3L, // 동일 userId
                3L,
                CouponStatus.ISSUED,
                System.currentTimeMillis(),
                null
            );
            CouponIssuedKafkaEvent event = new CouponIssuedKafkaEvent(history, coupon);
            couponKafkaProducer.publish(event);
        }

        // Then - 3개의 이벤트가 모두 발행되었는지 확인
        for (int i = 0; i < 3; i++) {
            ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                KafkaTopics.COUPON_EVENTS,
                Duration.ofSeconds(10)
            );

            assertThat(record).isNotNull();
            assertThat(record.key()).isEqualTo("3"); // 동일 userId
            assertThat(record.value().getUserId()).isEqualTo(3L);
            assertThat(record.value().getDiscountType()).isEqualTo("AMOUNT");
        }
    }
}