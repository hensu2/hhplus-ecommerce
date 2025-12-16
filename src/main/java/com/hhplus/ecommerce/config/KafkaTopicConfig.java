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
     * - 리플리케이션: 1 (로컬 개발 환경)
     * - 쿠폰 발급 요청을 처리하는 메인 토픽
     */
    @Bean
    public NewTopic couponEventsTopic() {
        return TopicBuilder.name("coupon-events")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000") // 7일 보관
            .config("compression.type", "snappy")
            .build();
    }

    /**
     * Dead Letter Queue (DLQ) - 쿠폰 이벤트
     * - 재시도 실패한 메시지 보관
     */
    @Bean
    public NewTopic couponEventsDlqTopic() {
        return TopicBuilder.name("coupon-events-dlq")
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "2592000000") // 30일 보관 (에러 분석용)
            .build();
    }
}