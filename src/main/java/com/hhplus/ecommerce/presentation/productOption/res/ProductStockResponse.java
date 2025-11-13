package com.hhplus.ecommerce.presentation.productOption.res;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

import java.util.List;
import java.util.stream.Collectors;

public record ProductStockResponse(
    Long productId,
    String productName,
    List<StockOptionResponse> options
) {
    public static ProductStockResponse from(ProductEntity product, List<ProductOptionEntity> options) {
        return new ProductStockResponse(
            product.getId(),
            product.getProductName(),
            options.stream()
                .map(StockOptionResponse::from)
                .collect(Collectors.toList())
        );
    }
}
