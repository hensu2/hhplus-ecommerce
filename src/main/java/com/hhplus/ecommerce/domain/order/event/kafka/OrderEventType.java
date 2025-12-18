package com.hhplus.ecommerce.domain.order.event.kafka;

/**
 * 주문 Kafka 이벤트 타입
 */
public enum OrderEventType {
    ORDER_CREATED,      // 주문 생성
    ORDER_COMPLETED,    // 주문 완료 (미래 확장용)
    ORDER_CANCELLED     // 주문 취소
}
