package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.domain.coupon.event.kafka.CouponIssuedKafkaEvent;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import com.hhplus.ecommerce.scheduler.CouponSyncScheduler;
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
    brokerProperties = {"listeners=PLAINTEXT://localhost:9095", "port=9095"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DisplayName("쿠폰 발급 Kafka E2E 테스트")
class CouponKafkaE2ETest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponSyncScheduler couponSyncScheduler;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, CouponIssuedKafkaEvent> consumer;

    @BeforeEach
    void setUp() {
        // Kafka Consumer 설정
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-group-e2e",
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
    @DisplayName("PERCENT 타입 쿠폰 발급 전체 플로우: 발급 요청 → Redis 저장 → DB 동기화 → Kafka 이벤트 발행")
    @Transactional
    void percentCouponIssueFullFlow_WithKafkaEvent() {
        // Given - PERCENT 타입 쿠폰 생성
        long now = System.currentTimeMillis();
        CouponEntity coupon = new CouponEntity(
            0L,
            "퍼센트 할인 쿠폰",
            DiscountType.PERCENT,
            20,
            10000,
            100000,
            50,
            now,
            now + 86400000L,
            now,
            now
        );
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // Redis 재고 초기화
        String stockKey = "coupon:stock:" + savedCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, "50");

        // When - Step 1: 쿠폰 발급 요청
        long userId = 2000L;
        IssueCouponResponse issueResponse = issueCouponUseCase.execute(userId, savedCoupon.getId());

        // Then - Step 1: 발급 요청 접수 확인
        assertThat(issueResponse).isNotNull();
        assertThat(issueResponse.getStatus()).isEqualTo("PENDING");
        assertThat(issueResponse.getUserId()).isEqualTo(userId);
        assertThat(issueResponse.getCouponId()).isEqualTo(savedCoupon.getId());

        // When - Step 2: Worker 및 DB 동기화
        String queueKey = "coupon:issue:queue:" + savedCoupon.getId();
        String requestJson = redisTemplate.opsForList().rightPop(queueKey);
        assertThat(requestJson).isNotNull();

        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();
        redisTemplate.opsForHash().put(pendingKey, String.valueOf(userId), requestJson.replace("requestedAt", "issuedAt"));

        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - Step 2: DB 저장 확인
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(userId, savedCoupon.getId());
            assertThat(history).isPresent();
            assertThat(history.get().getStatus()).isEqualTo(CouponStatus.ISSUED);
        });

        // Then - Step 3: Kafka 이벤트 확인
        ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.COUPON_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(userId));

        CouponIssuedKafkaEvent event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.getCouponId()).isEqualTo(savedCoupon.getId());
        assertThat(event.getCouponName()).isEqualTo("퍼센트 할인 쿠폰");
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getDiscountType()).isEqualTo("PERCENT");
        assertThat(event.getDiscountAmount()).isEqualTo(20);
        assertThat(event.getEventType().name()).isEqualTo("COUPON_ISSUED");
    }

    @Test
    @DisplayName("AMOUNT 타입 쿠폰 발급 전체 플로우: 발급 요청 → Redis 저장 → DB 동기화 → Kafka 이벤트 발행")
    @Transactional
    void amountCouponIssueFullFlow_WithKafkaEvent() {
        // Given - AMOUNT 타입 쿠폰 생성
        long now = System.currentTimeMillis();
        CouponEntity coupon = new CouponEntity(
            0L,
            "정액 할인 쿠폰",
            DiscountType.AMOUNT,
            5000,
            10000,
            100000,
            30,
            now,
            now + 86400000L,
            now,
            now
        );
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // Redis 재고 초기화
        String stockKey = "coupon:stock:" + savedCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, "30");

        // When - Step 1: 쿠폰 발급 요청
        long userId = 2100L;
        IssueCouponResponse issueResponse = issueCouponUseCase.execute(userId, savedCoupon.getId());

        // Then - Step 1: 발급 요청 접수 확인
        assertThat(issueResponse).isNotNull();
        assertThat(issueResponse.getStatus()).isEqualTo("PENDING");
        assertThat(issueResponse.getUserId()).isEqualTo(userId);
        assertThat(issueResponse.getCouponId()).isEqualTo(savedCoupon.getId());

        // When - Step 2: Worker 및 DB 동기화
        String queueKey = "coupon:issue:queue:" + savedCoupon.getId();
        String requestJson = redisTemplate.opsForList().rightPop(queueKey);
        assertThat(requestJson).isNotNull();

        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();
        redisTemplate.opsForHash().put(pendingKey, String.valueOf(userId), requestJson.replace("requestedAt", "issuedAt"));

        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - Step 2: DB 저장 확인
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(userId, savedCoupon.getId());
            assertThat(history).isPresent();
            assertThat(history.get().getStatus()).isEqualTo(CouponStatus.ISSUED);
        });

        // Then - Step 3: Kafka 이벤트 확인
        ConsumerRecord<String, CouponIssuedKafkaEvent> record = KafkaTestUtils.getSingleRecord(
            consumer,
            KafkaTopics.COUPON_EVENTS,
            Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(userId));

        CouponIssuedKafkaEvent event = record.value();
        assertThat(event).isNotNull();
        assertThat(event.getCouponId()).isEqualTo(savedCoupon.getId());
        assertThat(event.getCouponName()).isEqualTo("정액 할인 쿠폰");
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getDiscountType()).isEqualTo("AMOUNT");
        assertThat(event.getDiscountAmount()).isEqualTo(5000);
        assertThat(event.getEventType().name()).isEqualTo("COUPON_ISSUED");
    }

    @Test
    @DisplayName("PERCENT 타입 - 여러 사용자 동시 쿠폰 발급 시 모두 Kafka 이벤트로 발행")
    @Transactional
    void percentCoupon_MultipleConcurrentIssuances() throws Exception {
        // Given - PERCENT 쿠폰
        long now = System.currentTimeMillis();
        CouponEntity coupon = new CouponEntity(
            0L,
            "20% 할인 쿠폰",
            DiscountType.PERCENT,
            20,
            10000,
            100000,
            100,
            now,
            now + 86400000L,
            now,
            now
        );
        CouponEntity savedCoupon = couponRepository.save(coupon);

        String stockKey = "coupon:stock:" + savedCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, "100");

        int userCount = 3;

        // When - 여러 사용자 발급
        for (long userId = 3000; userId < 3000 + userCount; userId++) {
            IssueCouponResponse response = issueCouponUseCase.execute(userId, savedCoupon.getId());
            assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        // When - Worker 및 동기화
        String queueKey = "coupon:issue:queue:" + savedCoupon.getId();
        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();

        for (int i = 0; i < userCount; i++) {
            String requestJson = redisTemplate.opsForList().rightPop(queueKey);
            if (requestJson != null) {
                long userId = 3000 + i;
                redisTemplate.opsForHash().put(pendingKey, String.valueOf(userId), requestJson.replace("requestedAt", "issuedAt"));
            }
        }

        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - DB 확인
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            for (long userId = 3000; userId < 3000 + userCount; userId++) {
                Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(userId, savedCoupon.getId());
                assertThat(history).isPresent();
            }
        });

        // Then - Kafka 이벤트 확인
        for (int i = 0; i < userCount; i++) {
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
    @DisplayName("AMOUNT 타입 - 여러 사용자 동시 쿠폰 발급 시 모두 Kafka 이벤트로 발행")
    @Transactional
    void amountCoupon_MultipleConcurrentIssuances() throws Exception {
        // Given - AMOUNT 쿠폰
        long now = System.currentTimeMillis();
        CouponEntity coupon = new CouponEntity(
            0L,
            "3000원 할인 쿠폰",
            DiscountType.AMOUNT,
            3000,
            10000,
            100000,
            100,
            now,
            now + 86400000L,
            now,
            now
        );
        CouponEntity savedCoupon = couponRepository.save(coupon);

        String stockKey = "coupon:stock:" + savedCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, "100");

        int userCount = 3;

        // When - 여러 사용자 발급
        for (long userId = 4000; userId < 4000 + userCount; userId++) {
            IssueCouponResponse response = issueCouponUseCase.execute(userId, savedCoupon.getId());
            assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        // When - Worker 및 동기화
        String queueKey = "coupon:issue:queue:" + savedCoupon.getId();
        String pendingKey = "coupon:issued:pending:" + savedCoupon.getId();

        for (int i = 0; i < userCount; i++) {
            String requestJson = redisTemplate.opsForList().rightPop(queueKey);
            if (requestJson != null) {
                long userId = 4000 + i;
                redisTemplate.opsForHash().put(pendingKey, String.valueOf(userId), requestJson.replace("requestedAt", "issuedAt"));
            }
        }

        couponSyncScheduler.syncCouponIssuesToDB();

        // Then - DB 확인
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            for (long userId = 4000; userId < 4000 + userCount; userId++) {
                Optional<CouponHistoryEntity> history = couponRepository.findHistoryByUserIdAndCouponId(userId, savedCoupon.getId());
                assertThat(history).isPresent();
            }
        });

        // Then - Kafka 이벤트 확인
        for (int i = 0; i < userCount; i++) {
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