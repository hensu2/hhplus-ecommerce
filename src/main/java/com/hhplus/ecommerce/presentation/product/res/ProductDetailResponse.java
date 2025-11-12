package com.hhplus.ecommerce.presentation.product.res;

import java.util.List;

public record ProductDetailResponse(
    Long id,
    String productName,
    String content,
    Integer price,
    List<ProductOptionResponse> options,
    String createdAt
) {
}
