package com.hhplus.ecommerce.domain.order.event.kafka;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

/**
 * 주문 Kafka 이벤트 추상 클래스
 * 모든 주문 관련 Kafka 이벤트의 부모 클래스
 */
@Getter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedKafkaEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = OrderCancelledKafkaEvent.class, name = "ORDER_CANCELLED")
})
public abstract class OrderKafkaEvent {
    protected final OrderEventType eventType;
    protected final Long orderId;
    protected final Long timestamp;

    protected OrderKafkaEvent(OrderEventType eventType, Long orderId, Long timestamp) {
        this.eventType = eventType;
        this.orderId = orderId;
        this.timestamp = timestamp;
    }
}
