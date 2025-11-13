package com.hhplus.ecommerce.presentation.product.res;

public record ProductResponse(
    Long id,
    String productName,
    String content,
    Integer price,
    String createdAt
) {
}
