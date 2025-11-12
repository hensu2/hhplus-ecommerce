package com.hhplus.ecommerce.presentation.order.res;

public record OrderItemResponse(
    Long productId,
    Long productOptionId,
    String productName,
    String optionType,
    Integer quantity,
    Integer price
) {
}
