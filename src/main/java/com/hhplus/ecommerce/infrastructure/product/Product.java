package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.presentation.product.res.ProductResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record Product(
    long id,
    long createdUserId,
    String productName,
    String content,
    long price,
    long createdAt,
    long updatedAt
) {
    public ProductResponse toProductResponse() {
        String formattedDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return new ProductResponse(id, productName, content, (int) price, formattedDate);
    }
}