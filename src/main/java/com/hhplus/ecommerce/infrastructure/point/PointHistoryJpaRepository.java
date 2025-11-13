package com.hhplus.ecommerce.infrastructure.point;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryEntity, Long> {

    /**
     * 사용자의 포인트 내역 조회 (최신순)
     */
    @Query("SELECT ph FROM PointHistoryEntity ph WHERE ph.user.id = :userId ORDER BY ph.createdAt DESC")
    List<PointHistoryEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 사용자의 포인트 내역 조회 (최신순, 페이징)
     */
    @Query("SELECT ph FROM PointHistoryEntity ph WHERE ph.user.id = :userId ORDER BY ph.createdAt DESC")
    List<PointHistoryEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
}