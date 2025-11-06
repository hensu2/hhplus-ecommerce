package com.hhplus.ecommerce.domain.order;

public record OrderEntity(
    long id,
    long userId,
    int totalAmount,
    int discountAmount,
    int finalAmount,
    Long couponHistoryId,
    OrderStatus status,
    long createdAt,
    long updatedAt
) {
}
