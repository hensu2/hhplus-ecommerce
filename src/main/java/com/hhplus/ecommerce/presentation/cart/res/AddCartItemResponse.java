package com.hhplus.ecommerce.presentation.cart.res;

public record AddCartItemResponse(
        Long id,
        Long productId,
        String productName,
        Long optionId,
        String optionType,
        Integer quantity,
        Integer unitPrice,
        Integer totalPrice,
        String addedAt
) {
}
