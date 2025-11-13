package com.hhplus.ecommerce.application.point;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.PointHistoryRepository;
import com.hhplus.ecommerce.domain.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPointHistoryUseCase {

    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public List<PointHistoryEntity> execute(Long userId) {
        UserEntity.validateUserId(userId);
        return pointHistoryRepository.findByUserId(userId);
    }
}