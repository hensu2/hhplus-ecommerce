package com.hhplus.ecommerce.presentation.product.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.product.ProductEntity;

public record ProductResponse(
        Long id,
        String productName,
        String content,
        Integer price,
        String createdAt
) {
    public ProductResponse(ProductEntity product) {
        this(
            product.getId(),
            product.getProductName(),
            product.getContent(),
            product.getPrice().intValue(),
            DateTimeUtils.toLocalDateTime(product.getCreatedAt())
        );
    }
}
