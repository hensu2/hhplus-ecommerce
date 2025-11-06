package com.hhplus.ecommerce.domain.product;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.presentation.product.res.ProductResponse;
import com.hhplus.ecommerce.presentation.productOption.res.ProductStockResponse;
import com.hhplus.ecommerce.presentation.productOption.res.StockOptionResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public record ProductEntity(
    long id,
    long createdUserId,
    String productName,
    String content,
    long price,
    long createdAt,
    long updatedAt
) {
    public static void validateProductId(Long productId) {
        if (productId == null) {
            throw new InvalidInputException("Product ID cannot be null");
        }
        if (productId <= 0) {
            throw new InvalidInputException("Product ID must be greater than 0");
        }
    }

    public String getFormattedCreatedAt() {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public ProductResponse toProductResponse() {
        return new ProductResponse(id, productName, content, (int) price, getFormattedCreatedAt());
    }

    public ProductStockResponse toProductStockResponse(List<ProductOptionEntity> options) {
        List<StockOptionResponse> stockOptions = options.stream()
            .map(ProductOptionEntity::toStockOptionResponse)
            .collect(Collectors.toList());

        return new ProductStockResponse(id, productName, stockOptions);
    }
}