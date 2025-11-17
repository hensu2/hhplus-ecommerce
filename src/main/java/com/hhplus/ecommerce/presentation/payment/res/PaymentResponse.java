package com.hhplus.ecommerce.presentation.payment.res;

public record PaymentResponse(
    Long paymentId,
    Long orderId,
    Long userId,
    Integer amount,
    String status,
    String createdAt
) {
}
