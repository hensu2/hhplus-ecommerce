package com.hhplus.ecommerce.presentation.event.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.event.FailedEventEntity;

public record FailedEventResponse(
        Long id,
        String eventType,
        Long orderId,
        String eventPayload,
        String errorMessage,
        Integer retryCount,
        String status,
        String createdAt,
        String updatedAt
) {
    public FailedEventResponse(FailedEventEntity entity) {
        this(
            entity.getId(),
            entity.getEventType().name(),
            entity.getOrderId(),
            entity.getEventPayload(),
            entity.getErrorMessage(),
            entity.getRetryCount(),
            entity.getStatus().name(),
            DateTimeUtils.toLocalDateTime(entity.getCreatedAt()),
            DateTimeUtils.toLocalDateTime(entity.getUpdatedAt())
        );
    }
}