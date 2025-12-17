package com.hhplus.ecommerce.domain.order.event.kafka;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 취소 Kafka 이벤트
 * 주문이 취소되었을 때 Kafka로 발행되는 이벤트
 */
@Getter
public class OrderCancelledKafkaEvent extends OrderKafkaEvent {
    private final Long userId;
    private final List<OrderItemDto> orderItems;
    private final Long cancelledAt;

    public OrderCancelledKafkaEvent(OrderEntity order, List<OrderItemEntity> orderItems) {
        super(OrderEventType.ORDER_CANCELLED, order.getId(), order.getUpdatedAt());
        this.userId = order.getUserId();
        this.orderItems = orderItems.stream()
            .map(OrderItemDto::from)
            .collect(Collectors.toList());
        this.cancelledAt = order.getUpdatedAt();
    }
}
