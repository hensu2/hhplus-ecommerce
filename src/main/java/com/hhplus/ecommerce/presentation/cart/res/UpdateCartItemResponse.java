package com.hhplus.ecommerce.presentation.cart.res;

public record UpdateCartItemResponse(
    Long id,
    Long productId,
    Integer quantity,
    Integer totalPrice,
    String updatedAt
) {
}
