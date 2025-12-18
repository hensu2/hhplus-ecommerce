package com.hhplus.ecommerce.infrastructure.stockAnalytics;

import com.hhplus.ecommerce.domain.stockAnalytics.StockAnalyticsEntity;

import java.util.List;
import java.util.Optional;

public interface StockAnalyticsRepository {
    StockAnalyticsEntity save(StockAnalyticsEntity entity);
    Optional<StockAnalyticsEntity> findByProductOptionIdAndDate(Long productOptionId, String date);
    List<StockAnalyticsEntity> findByDate(String date);
}
