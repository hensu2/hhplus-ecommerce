package com.hhplus.ecommerce.domain.order.event.kafka;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 생성 Kafka 이벤트
 * 주문이 생성되었을 때 Kafka로 발행되는 이벤트
 */
@Getter
public class OrderCreatedKafkaEvent extends OrderKafkaEvent {
    private final Long userId;
    private final List<OrderItemDto> orderItems;
    private final Integer totalAmount;
    private final Integer discountAmount;
    private final Integer finalAmount;
    private final Long couponHistoryId;

    public OrderCreatedKafkaEvent(OrderEntity order, List<OrderItemEntity> orderItems) {
        super(OrderEventType.ORDER_CREATED, order.getId(), order.getOrderedAt());
        this.userId = order.getUserId();
        this.orderItems = orderItems.stream()
            .map(OrderItemDto::from)
            .collect(Collectors.toList());
        this.totalAmount = order.getTotalAmount();
        this.discountAmount = order.getDiscountAmount();
        this.finalAmount = order.getFinalAmount();
        this.couponHistoryId = order.getCouponHistoryId();
    }
}
