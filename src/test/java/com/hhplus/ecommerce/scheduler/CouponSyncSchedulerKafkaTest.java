package com.hhplus.ecommerce.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.application.coupon.dto.CouponIssuePending;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.domain.coupon.event.kafka.CouponIssuedKafkaEvent;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaTopics.COUPON_EVENTS},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("CouponSyncScheduler Kafka 통합 테스트")
class CouponSyncSchedulerKafkaTest {

    @Autowired
    private CouponSyncScheduler couponSyncScheduler;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, CouponIssuedKafkaEvent> consumer;

    @BeforeEach
    void setUp() {
        // Kafka Consumer 설정
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-group-scheduler",
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

        // Redis 정리
        redisTemplate.delete(redisTemplate.keys("coupon:*"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
        // Redis 정리
        redisTemplate.delete(redisTemplate.keys("coupon:*"));
    }

    @Test
    @DisplayName("PERCENT 타입 - DB 동기화 시 Kafka 이벤트가 발행된다")
    void percentCoupon_SyncToDb_PublishesKafkaEvent() throws Exception {
        // Given - PERCENT 쿠폰 생성
        CouponEntity coupon = new CouponEntity(
            0L,
            "15% 할인 쿠폰",
            DiscountType.PERCENT,
            15,
            10000,
            100000,
            100,
            System.currentTimeMillis(),
            System.currentTimeMillis() + 86400000L,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // Given - Redis에 pending 데이터 추가
        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();
        CouponIssuePending pending = new CouponIssuePending(
            0L,
            999L,
            savedCoupon.getId(),
            "ISSUED",
            System.currentTimeMillis()
        );
        String pendingJson = objectMapper.writeValueAsString(pending);
        redisTemplate.opsForHash().put(pendingKey, "999", pendingJson);

        // When - DB 동기화 실행
        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - DB 저장 확인
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(999L, savedCoupon.getId());
            assertThat(history).isPresent();
            assertThat(history.get().getUserId()).isEqualTo(999L);
            assertThat(history.get().getStatus()).isEqualTo(CouponStatus.ISSUED);
        });

        // Then - Kafka 이벤트 확인
        ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.COUPON_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("999");
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getDiscountType()).isEqualTo("PERCENT");
        assertThat(record.value().getDiscountAmount()).isEqualTo(15);
    }

    @Test
    @DisplayName("AMOUNT 타입 - DB 동기화 시 Kafka 이벤트가 발행된다")
    void amountCoupon_SyncToDb_PublishesKafkaEvent() throws Exception {
        // Given - AMOUNT 쿠폰 생성
        CouponEntity coupon = new CouponEntity(
            0L,
            "5000원 할인 쿠폰",
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
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // Given - Redis에 pending 데이터 추가
        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();
        CouponIssuePending pending = new CouponIssuePending(
            0L,
            888L,
            savedCoupon.getId(),
            "ISSUED",
            System.currentTimeMillis()
        );
        String pendingJson = objectMapper.writeValueAsString(pending);
        redisTemplate.opsForHash().put(pendingKey, "888", pendingJson);

        // When - DB 동기화 실행
        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - DB 저장 확인
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(888L, savedCoupon.getId());
            assertThat(history).isPresent();
            assertThat(history.get().getUserId()).isEqualTo(888L);
            assertThat(history.get().getStatus()).isEqualTo(CouponStatus.ISSUED);
        });

        // Then - Kafka 이벤트 확인
        ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.COUPON_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("888");
        assertThat(record.value()).isNotNull();
        assertThat(record.value().getDiscountType()).isEqualTo("AMOUNT");
        assertThat(record.value().getDiscountAmount()).isEqualTo(5000);
    }

    @Test
    @DisplayName("PERCENT 타입 - 여러 쿠폰 발급이 DB 동기화 시 모두 Kafka 이벤트로 발행")
    void percentCoupon_SyncMultiple_PublishesAllKafkaEvents() throws Exception {
        // Given - PERCENT 쿠폰 생성
        CouponEntity coupon = new CouponEntity(
            0L,
            "20% 할인 쿠폰",
            DiscountType.PERCENT,
            20,
            10000,
            100000,
            100,
            System.currentTimeMillis(),
            System.currentTimeMillis() + 86400000L,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // Given - Redis에 3개의 pending 데이터 추가
        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();
        for (long userId = 1001; userId <= 1003; userId++) {
            CouponIssuePending pending = new CouponIssuePending(
                0L,
                userId,
                savedCoupon.getId(),
                "ISSUED",
                System.currentTimeMillis()
            );
            String pendingJson = objectMapper.writeValueAsString(pending);
            redisTemplate.opsForHash().put(pendingKey, String.valueOf(userId), pendingJson);
        }

        // When - DB 동기화 실행
        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - DB 저장 확인
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            for (long userId = 1001; userId <= 1003; userId++) {
                Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(userId, savedCoupon.getId());
                assertThat(history).isPresent();
            }
        });

        // Then - Kafka 이벤트 3개 확인
        for (int i = 0; i < 3; i++) {
            ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                KafkaTopics.COUPON_EVENTS,
                Duration.ofSeconds(10)
            );

            assertThat(record).isNotNull();
            assertThat(record.value().getDiscountType()).isEqualTo("PERCENT");
            assertThat(record.value().getDiscountAmount()).isEqualTo(20);
        }
    }

    @Test
    @DisplayName("AMOUNT 타입 - 여러 쿠폰 발급이 DB 동기화 시 모두 Kafka 이벤트로 발행")
    void amountCoupon_SyncMultiple_PublishesAllKafkaEvents() throws Exception {
        // Given - AMOUNT 쿠폰 생성
        CouponEntity coupon = new CouponEntity(
            0L,
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
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // Given - Redis에 3개의 pending 데이터 추가
        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();
        for (long userId = 2001; userId <= 2003; userId++) {
            CouponIssuePending pending = new CouponIssuePending(
                0L,
                userId,
                savedCoupon.getId(),
                "ISSUED",
                System.currentTimeMillis()
            );
            String pendingJson = objectMapper.writeValueAsString(pending);
            redisTemplate.opsForHash().put(pendingKey, String.valueOf(userId), pendingJson);
        }

        // When - DB 동기화 실행
        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - DB 저장 확인
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            for (long userId = 2001; userId <= 2003; userId++) {
                Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(userId, savedCoupon.getId());
                assertThat(history).isPresent();
            }
        });

        // Then - Kafka 이벤트 3개 확인
        for (int i = 0; i < 3; i++) {
            ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                KafkaTopics.COUPON_EVENTS,
                Duration.ofSeconds(10)
            );

            assertThat(record).isNotNull();
            assertThat(record.value().getDiscountType()).isEqualTo("AMOUNT");
            assertThat(record.value().getDiscountAmount()).isEqualTo(3000);
        }
    }
}