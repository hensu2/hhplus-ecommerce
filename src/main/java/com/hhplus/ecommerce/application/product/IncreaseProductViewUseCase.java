package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncreaseProductViewUseCase {

    private final ProductStatisticsRepository productStatisticsRepository;

    public void execute(long productId) {
        // 조회수를 증가
        ProductStatisticsEntity statistics = productStatisticsRepository.findByProductId(productId)
                .orElse(new ProductStatisticsEntity(productId, 0L, 0L, System.currentTimeMillis()));

        statistics.increaseViewCount();
        productStatisticsRepository.save(statistics);
    }
}
