package com.hhplus.ecommerce.presentation.product.res;

public record PopularProductResponse(
    Long productId,
    String productName,
    Integer price,
    Long viewCount,
    Long salesCount,
    Long popularityScore
) {
}
