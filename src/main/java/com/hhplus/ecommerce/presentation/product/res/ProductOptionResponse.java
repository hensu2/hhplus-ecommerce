package com.hhplus.ecommerce.presentation.product.res;

public record ProductOptionResponse(
        Long id,
        String optionType,
        Integer additionalPrice,
        Integer stock
) {
}
