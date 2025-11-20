package com.hhplus.ecommerce.infrastructure.user.jpa;

import com.hhplus.ecommerce.domain.user.PointHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryEntity, Long> {
    List<PointHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
