package com.hhplus.ecommerce.presentation.order.res;

public record OrderListItemResponse(
        Long orderId,
        String status,
        Integer totalAmount,
        Integer finalAmount,
        Integer itemCount,
        String orderedAt
) {
}
