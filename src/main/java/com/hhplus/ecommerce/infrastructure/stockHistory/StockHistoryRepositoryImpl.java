package com.hhplus.ecommerce.infrastructure.stockHistory;

import com.hhplus.ecommerce.domain.stockHistory.StockHistoryEntity;
import com.hhplus.ecommerce.infrastructure.stockHistory.jpa.StockHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockHistoryRepositoryImpl implements StockHistoryRepository {

    private final StockHistoryJpaRepository jpaRepository;

    @Override
    public StockHistoryEntity save(StockHistoryEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<StockHistoryEntity> findByProductOptionId(Long productOptionId) {
        return jpaRepository.findByProductOptionIdOrderByCreatedAtDesc(productOptionId);
    }

    @Override
    public List<StockHistoryEntity> findByProductId(Long productId) {
        return jpaRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}
