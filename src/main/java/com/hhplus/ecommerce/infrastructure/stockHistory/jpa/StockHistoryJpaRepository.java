package com.hhplus.ecommerce.infrastructure.stockHistory.jpa;

import com.hhplus.ecommerce.domain.stockHistory.StockHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockHistoryJpaRepository extends JpaRepository<StockHistoryEntity, Long> {
    List<StockHistoryEntity> findByProductOptionIdOrderByCreatedAtDesc(Long productOptionId);
    List<StockHistoryEntity> findByProductIdOrderByCreatedAtDesc(Long productId);
}
