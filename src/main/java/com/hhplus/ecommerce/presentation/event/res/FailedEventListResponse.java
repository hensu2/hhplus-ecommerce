package com.hhplus.ecommerce.presentation.event.res;

import com.hhplus.ecommerce.domain.event.FailedEventEntity;

import java.util.List;

public record FailedEventListResponse(
        List<FailedEventResponse> events,
        Integer totalCount
) {
    public FailedEventListResponse(List<FailedEventEntity> entities) {
        this(
            entities.stream().map(FailedEventResponse::new).toList(),
            entities.size()
        );
    }
}