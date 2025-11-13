package com.hhplus.ecommerce.presentation.point.res;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long id,
        Long userId,
        Long amount,
        String transactionType,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PointHistoryResponse from(PointHistoryEntity entity) {
        return new PointHistoryResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getAmount(),
                entity.getTransactionType().name(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}