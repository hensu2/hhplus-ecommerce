package com.hhplus.ecommerce.presentation.product.res;

import com.hhplus.ecommerce.domain.product.ProductEntity;

import java.time.LocalDateTime;

public record ProductDetailResponse(
        Long id,
        Long createdUserId,
        String productName,
        String content,
        Long price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductDetailResponse from(ProductEntity entity) {
        return new ProductDetailResponse(
                entity.getId(),
                entity.getCreatedUser().getId(),
                entity.getProductName(),
                entity.getContent(),
                entity.getPrice(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
