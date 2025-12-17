package com.hhplus.ecommerce.infrastructure.kafka;

/**
 * Kafka 토픽명 상수 정의
 */
public final class KafkaTopics {

    private KafkaTopics() {
        throw new IllegalStateException("Utility class");
    }

    // 쿠폰 도메인 토픽
    public static final String COUPON_EVENTS = "coupon-events";
    public static final String COUPON_EVENTS_DLQ = "coupon-events-dlq";

    // 주문 도메인 토픽
    public static final String ORDER_EVENTS = "order-events";
    public static final String ORDER_EVENTS_DLQ = "order-events-dlq";
}