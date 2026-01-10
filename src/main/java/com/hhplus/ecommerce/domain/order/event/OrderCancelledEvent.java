package com.hhplus.ecommerce.domain.order.event;

import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private List<OrderItemEntity> orderItems;
    private Long cancelledAt;
}