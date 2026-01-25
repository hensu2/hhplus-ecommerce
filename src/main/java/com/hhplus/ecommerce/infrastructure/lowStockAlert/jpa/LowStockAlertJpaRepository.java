package com.hhplus.ecommerce.infrastructure.lowStockAlert.jpa;

import com.hhplus.ecommerce.domain.lowStockAlert.LowStockAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LowStockAlertJpaRepository extends JpaRepository<LowStockAlertEntity, Long> {
    List<LowStockAlertEntity> findByIsResolvedFalseOrderByCreatedAtDesc();
    Optional<LowStockAlertEntity> findTopByProductOptionIdAndIsResolvedFalseOrderByCreatedAtDesc(Long productOptionId);
}
