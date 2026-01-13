package com.hhplus.ecommerce.infrastructure.event;

import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.event.FailedEventStatus;
import com.hhplus.ecommerce.infrastructure.event.jpa.FailedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FailedEventRepositoryImpl implements FailedEventRepository {

    private final FailedEventJpaRepository failedEventJpaRepository;

    @Override
    public FailedEventEntity save(FailedEventEntity failedEvent) {
        return failedEventJpaRepository.save(failedEvent);
    }

    @Override
    public Optional<FailedEventEntity> findById(Long id) {
        return failedEventJpaRepository.findById(id);
    }

    @Override
    public List<FailedEventEntity> findByStatus(FailedEventStatus status) {
        return failedEventJpaRepository.findByStatus(status);
    }

    @Override
    public List<FailedEventEntity> findAll() {
        return failedEventJpaRepository.findAll();
    }
}