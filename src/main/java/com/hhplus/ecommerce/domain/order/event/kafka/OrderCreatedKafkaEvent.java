package com.hhplus.ecommerce.domain.order.event.kafka;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 생성 Kafka 이벤트
 * 주문이 생성되었을 때 Kafka로 발행되는 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedKafkaEvent extends OrderKafkaEvent {
    private Long userId;
    private List<OrderItemDto> orderItems;
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private Long couponHistoryId;

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
