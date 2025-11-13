package com.hhplus.ecommerce.presentation.payment.req;

public record ProcessPaymentRequest(
    Long orderId,
    Long userId,
    Integer amount
) {
}
