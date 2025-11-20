package com.hhplus.ecommerce.presentation.user.res;

public record PointHistoryItemResponse(
        Long id,
        Integer amount,
        String transactionType,
        String description,
        String createdAt
) {
}
