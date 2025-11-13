package com.hhplus.ecommerce.presentation.product.req;

public record CreateProductRequest(
        Long createdUserId,
        String productName,
        String content,
        Long price
) {
}
