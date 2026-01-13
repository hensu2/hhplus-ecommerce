package com.hhplus.ecommerce.infrastructure.stockAnalytics.jpa;

import com.hhplus.ecommerce.domain.stockAnalytics.StockAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockAnalyticsJpaRepository extends JpaRepository<StockAnalyticsEntity, Long> {
    Optional<StockAnalyticsEntity> findByProductOptionIdAndDate(Long productOptionId, String date);
    List<StockAnalyticsEntity> findByDateOrderByTotalDecreasedDesc(String date);
}
