package com.hhplus.ecommerce.infrastructure.lowStockAlert;

import com.hhplus.ecommerce.domain.lowStockAlert.LowStockAlertEntity;
import com.hhplus.ecommerce.infrastructure.lowStockAlert.jpa.LowStockAlertJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LowStockAlertRepositoryImpl implements LowStockAlertRepository {

    private final LowStockAlertJpaRepository jpaRepository;

    @Override
    public LowStockAlertEntity save(LowStockAlertEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<LowStockAlertEntity> findUnresolvedAlerts() {
        return jpaRepository.findByIsResolvedFalseOrderByCreatedAtDesc();
    }

    @Override
    public Optional<LowStockAlertEntity> findLatestUnresolvedAlert(Long productOptionId) {
        return jpaRepository.findTopByProductOptionIdAndIsResolvedFalseOrderByCreatedAtDesc(productOptionId);
    }
}
