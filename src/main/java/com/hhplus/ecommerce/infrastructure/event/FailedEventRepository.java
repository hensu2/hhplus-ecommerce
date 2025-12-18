package com.hhplus.ecommerce.infrastructure.event;

import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.event.FailedEventStatus;

import java.util.List;
import java.util.Optional;

public interface FailedEventRepository {
    FailedEventEntity save(FailedEventEntity failedEvent);
    Optional<FailedEventEntity> findById(Long id);
    List<FailedEventEntity> findByStatus(FailedEventStatus status);
    List<FailedEventEntity> findAll();

    default FailedEventEntity getOrThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new IllegalArgumentException("실패 이벤트를 찾을 수 없습니다. ID: " + id));
    }
}