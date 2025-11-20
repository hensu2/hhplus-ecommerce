package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.PointHistoryEntity;
import com.hhplus.ecommerce.infrastructure.user.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPointHistoryUseCase {

    private final PointHistoryRepository pointHistoryRepository;

    public List<PointHistoryEntity> execute(Long userId) {
        return pointHistoryRepository.findByUserId(userId);
    }
}
