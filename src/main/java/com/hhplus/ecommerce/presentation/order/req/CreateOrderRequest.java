package com.hhplus.ecommerce.presentation.order.req;

import java.util.List;

public record CreateOrderRequest(
        Long userId,
        List<OrderItemRequest> items,
        Long couponHistoryId
) {
}
