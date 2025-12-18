package com.hhplus.ecommerce.domain.order.event.kafka;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 취소 Kafka 이벤트
 * 주문이 취소되었을 때 Kafka로 발행되는 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledKafkaEvent extends OrderKafkaEvent {
    private Long userId;
    private List<OrderItemDto> orderItems;
    private Long cancelledAt;

    public OrderCancelledKafkaEvent(OrderEntity order, List<OrderItemEntity> orderItems) {
        super(OrderEventType.ORDER_CANCELLED, order.getId(), order.getUpdatedAt());
        this.userId = order.getUserId();
        this.orderItems = orderItems.stream()
            .map(OrderItemDto::from)
            .collect(Collectors.toList());
        this.cancelledAt = order.getUpdatedAt();
    }
}
