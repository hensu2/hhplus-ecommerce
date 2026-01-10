package com.hhplus.ecommerce.infrastructure.event.jpa;

import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.event.FailedEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedEventJpaRepository extends JpaRepository<FailedEventEntity, Long> {
    List<FailedEventEntity> findByStatus(FailedEventStatus status);
}