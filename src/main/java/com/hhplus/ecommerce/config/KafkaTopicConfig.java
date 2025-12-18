package com.hhplus.ecommerce.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * 쿠폰 이벤트 토픽
     * - 파티션: 3
     * - 리플리케이션: 3 (고가용성)
     * - Min ISR: 2 (최소 2개 브로커 확인)
     * - 쿠폰 발급 요청을 처리하는 메인 토픽
     */
    @Bean
    public NewTopic couponEventsTopic() {
        return TopicBuilder.name("coupon-events")
            .partitions(3)
            .replicas(3)
            .config("retention.ms", "604800000") // 7일 보관
            .config("compression.type", "snappy")
            .config("min.insync.replicas", "2") // 최소 2개 브로커 확인 필요
            .build();
    }

    /**
     * Dead Letter Queue (DLQ) - 쿠폰 이벤트
     * - 재시도 실패한 메시지 보관
     * - 리플리케이션: 3 (데이터 손실 방지)
     */
    @Bean
    public NewTopic couponEventsDlqTopic() {
        return TopicBuilder.name("coupon-events-dlq")
            .partitions(1)
            .replicas(3)
            .config("retention.ms", "2592000000") // 30일 보관 (에러 분석용)
            .config("min.insync.replicas", "2")
            .build();
    }

    /**
     * 주문 이벤트 토픽
     * - 파티션: 3
     * - 리플리케이션: 3 (고가용성)
     * - Min ISR: 2 (최소 2개 브로커 확인)
     * - 주문 생성/취소 이벤트를 처리하는 메인 토픽
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order-events")
            .partitions(3)
            .replicas(3)
            .config("retention.ms", "604800000") // 7일 보관
            .config("compression.type", "snappy")
            .config("min.insync.replicas", "2")
            .build();
    }

    /**
     * Dead Letter Queue (DLQ) - 주문 이벤트
     * - 재시도 실패한 메시지 보관
     * - 리플리케이션: 3 (데이터 손실 방지)
     */
    @Bean
    public NewTopic orderEventsDlqTopic() {
        return TopicBuilder.name("order-events-dlq")
            .partitions(1)
            .replicas(3)
            .config("retention.ms", "2592000000") // 30일 보관 (에러 분석용)
            .config("min.insync.replicas", "2")
            .build();
    }

    /**
     * 재고 이벤트 토픽
     * - 파티션: 3
     * - 리플리케이션: 3 (고가용성)
     * - Min ISR: 2 (최소 2개 브로커 확인)
     * - 재고 증가/감소/설정 이벤트를 처리하는 메인 토픽
     */
    @Bean
    public NewTopic stockEventsTopic() {
        return TopicBuilder.name("stock-events")
            .partitions(3)
            .replicas(3)
            .config("retention.ms", "604800000") // 7일 보관
            .config("compression.type", "snappy")
            .config("min.insync.replicas", "2")
            .build();
    }

    /**
     * Dead Letter Queue (DLQ) - 재고 이벤트
     * - 재시도 실패한 메시지 보관
     * - 리플리케이션: 3 (데이터 손실 방지)
     */
    @Bean
    public NewTopic stockEventsDlqTopic() {
        return TopicBuilder.name("stock-events-dlq")
            .partitions(1)
            .replicas(3)
            .config("retention.ms", "2592000000") // 30일 보관 (에러 분석용)
            .config("min.insync.replicas", "2")
            .build();
    }
}