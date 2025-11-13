package com.hhplus.ecommerce.domain.point;

import java.util.List;
import java.util.Optional;

public interface PointHistoryRepository {
    PointHistoryEntity save(PointHistoryEntity pointHistory);
    Optional<PointHistoryEntity> findById(Long id);
    List<PointHistoryEntity> findByUserId(Long userId);
}