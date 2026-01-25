package com.hhplus.ecommerce.infrastructure.stockHistory;

import com.hhplus.ecommerce.domain.stockHistory.StockHistoryEntity;

import java.util.List;

public interface StockHistoryRepository {
    StockHistoryEntity save(StockHistoryEntity entity);
    List<StockHistoryEntity> findByProductOptionId(Long productOptionId);
    List<StockHistoryEntity> findByProductId(Long productId);
}
