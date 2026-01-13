package com.hhplus.ecommerce.infrastructure.lowStockAlert;

import com.hhplus.ecommerce.domain.lowStockAlert.LowStockAlertEntity;

import java.util.List;
import java.util.Optional;

public interface LowStockAlertRepository {
    LowStockAlertEntity save(LowStockAlertEntity entity);
    List<LowStockAlertEntity> findUnresolvedAlerts();
    Optional<LowStockAlertEntity> findLatestUnresolvedAlert(Long productOptionId);
}
