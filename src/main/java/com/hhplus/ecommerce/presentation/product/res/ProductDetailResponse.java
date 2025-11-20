package com.hhplus.ecommerce.presentation.product.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

import java.util.List;

public record ProductDetailResponse(
        Long id,
        String productName,
        String content,
        Integer price,
        List<ProductOptionResponse> options,
        String createdAt
) {
    public ProductDetailResponse(ProductEntity product, List<ProductOptionEntity> optionEntities) {
        this(
            product.getId(),
            product.getProductName(),
            product.getContent(),
            product.getPrice().intValue(),
            optionEntities.stream()
                .map(option -> new ProductOptionResponse(
                    option.getId(),
                    option.getOptionType(),
                    option.getAdditionalPrice().intValue(),
                    option.getStock().intValue()
                ))
                .toList(),
            DateTimeUtils.toLocalDateTime(product.getCreatedAt())
        );
    }
}
