package com.hhplus.ecommerce.presentation.product.res;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;

public record PopularProductResponse(
        Long productId,
        String productName,
        Integer price,
        Long viewCount,
        Long salesCount,
        Long popularityScore
) {
    public PopularProductResponse(ProductEntity product, ProductStatisticsEntity statistics) {
        this(
            product.getId(),
            product.getProductName(),
            product.getPrice().intValue(),
            statistics.getViewCount(),
            statistics.getSalesCount(),
            statistics.getPopularityScore()
        );
    }
}
