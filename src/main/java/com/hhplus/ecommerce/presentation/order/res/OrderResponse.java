package com.hhplus.ecommerce.presentation.order.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;

import java.util.List;

public record OrderResponse(
        Long orderId,
        Long userId,
        Integer totalAmount,
        Integer discountAmount,
        Integer finalAmount,
        Long couponHistoryId,
        String status,
        List<OrderItemResponse> items,
        String createdAt
) {
    public OrderResponse(OrderEntity order, List<OrderItemEntity> items) {
        this(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getFinalAmount(),
            order.getCouponHistoryId(),
            order.getStatus().name(),
            items.stream().map(OrderItemResponse::new).toList(),
            DateTimeUtils.toLocalDateTime(order.getCreatedAt())
        );
    }
}
