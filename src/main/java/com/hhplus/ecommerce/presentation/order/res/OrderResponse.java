package com.hhplus.ecommerce.presentation.order.res;

import java.util.List;

public record OrderResponse(
    Long orderId,
    Long userId,
    Integer totalAmount,
    Integer discountAmount,
    Integer finalAmount,
    String status,
    List<OrderItemResponse> items,
    String createdAt
) {
}
