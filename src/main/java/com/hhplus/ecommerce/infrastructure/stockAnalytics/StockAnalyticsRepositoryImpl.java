package com.hhplus.ecommerce.infrastructure.stockAnalytics;

import com.hhplus.ecommerce.domain.stockAnalytics.StockAnalyticsEntity;
import com.hhplus.ecommerce.infrastructure.stockAnalytics.jpa.StockAnalyticsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockAnalyticsRepositoryImpl implements StockAnalyticsRepository {

    private final StockAnalyticsJpaRepository jpaRepository;

    @Override
    public StockAnalyticsEntity save(StockAnalyticsEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<StockAnalyticsEntity> findByProductOptionIdAndDate(Long productOptionId, String date) {
        return jpaRepository.findByProductOptionIdAndDate(productOptionId, date);
    }

    @Override
    public List<StockAnalyticsEntity> findByDate(String date) {
        return jpaRepository.findByDateOrderByTotalDecreasedDesc(date);
    }
}
