package com.hhplus.ecommerce.presentation.productOption.res;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

import java.time.LocalDateTime;

public record ProductOptionResponse(
    Long id,
    Long productId,
    String optionType,
    Long additionalPrice,
    Long stock,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ProductOptionResponse from(ProductOptionEntity entity) {
        return new ProductOptionResponse(
            entity.getId(),
            entity.getProduct().getId(),
            entity.getOptionType(),
            entity.getAdditionalPrice(),
            entity.getStock(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}