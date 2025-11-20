package com.hhplus.ecommerce.presentation.cart.res;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        Integer totalAmount
) {
}
