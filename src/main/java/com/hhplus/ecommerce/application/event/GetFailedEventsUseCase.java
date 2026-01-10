package com.hhplus.ecommerce.application.event;

import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.event.FailedEventStatus;
import com.hhplus.ecommerce.infrastructure.event.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetFailedEventsUseCase {

    private final FailedEventRepository failedEventRepository;

    public List<FailedEventEntity> execute() {
        return failedEventRepository.findAll();
    }

    public List<FailedEventEntity> executeByStatus(FailedEventStatus status) {
        return failedEventRepository.findByStatus(status);
    }

    public FailedEventEntity executeById(Long id) {
        return failedEventRepository.getOrThrow(id);
    }
}