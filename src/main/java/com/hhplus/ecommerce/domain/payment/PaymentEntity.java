package com.hhplus.ecommerce.domain.payment;

public record PaymentEntity(
    long id,
    long orderId,
    long userId,
    int amount,
    PaymentStatus status,
    long createdAt,
    long updatedAt
) {
}
