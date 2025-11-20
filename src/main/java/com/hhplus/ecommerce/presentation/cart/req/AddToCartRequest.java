package com.hhplus.ecommerce.presentation.cart.req;

public record AddToCartRequest(
        Long productId,
        Long optionId,
        Integer quantity
) {
}