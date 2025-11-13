package com.hhplus.ecommerce.presentation.order.req;

public record OrderItemRequest(
    Long productOptionId,
    Integer quantity
) {
}
