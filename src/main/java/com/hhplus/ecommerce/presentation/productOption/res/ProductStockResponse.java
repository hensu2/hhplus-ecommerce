package com.hhplus.ecommerce.presentation.productOption.res;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

import java.util.List;

public record ProductStockResponse(
        Long productId,
        String productName,
        List<StockOptionResponse> options
) {
    public ProductStockResponse(ProductEntity product, List<ProductOptionEntity> optionEntities) {
        this(
            product.getId(),
            product.getProductName(),
            optionEntities.stream()
                .map(StockOptionResponse::new)
                .toList()
        );
    }
}