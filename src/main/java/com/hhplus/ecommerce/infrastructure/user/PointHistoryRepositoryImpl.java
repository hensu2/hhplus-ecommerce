package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.PointHistoryEntity;
import com.hhplus.ecommerce.infrastructure.user.jpa.PointHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public List<PointHistoryEntity> findByUserId(Long userId) {
        return pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public PointHistoryEntity save(PointHistoryEntity history) {
        return pointHistoryJpaRepository.save(history);
    }
}
