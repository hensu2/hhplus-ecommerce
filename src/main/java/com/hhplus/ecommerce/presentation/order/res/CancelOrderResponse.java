package com.hhplus.ecommerce.presentation.order.res;

public record CancelOrderResponse(
    Long orderId,
    String status,
    Integer refundAmount,
    Integer refundPoint,
    Boolean couponRestored,
    String cancelledAt
) {
}
