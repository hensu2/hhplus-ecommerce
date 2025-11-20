package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.PointHistoryEntity;

import java.util.List;

public interface PointHistoryRepository {
    List<PointHistoryEntity> findByUserId(Long userId);
    PointHistoryEntity save(PointHistoryEntity history);
}
