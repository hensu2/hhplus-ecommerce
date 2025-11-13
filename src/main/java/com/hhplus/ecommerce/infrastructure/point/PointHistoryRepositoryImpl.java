package com.hhplus.ecommerce.infrastructure.point;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public PointHistoryEntity save(PointHistoryEntity pointHistory) {
        return pointHistoryJpaRepository.save(pointHistory);
    }

    @Override
    public Optional<PointHistoryEntity> findById(Long id) {
        return pointHistoryJpaRepository.findById(id);
    }

    @Override
    public List<PointHistoryEntity> findByUserId(Long userId) {
        return pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}