package com.hhplus.ecommerce.domain.order;

public record OrderItemEntity(
    long id,
    long orderId,
    long productId,
    long productOptionId,
    String productName,
    String optionType,
    int quantity,
    int price,
    long createdAt
) {
}
