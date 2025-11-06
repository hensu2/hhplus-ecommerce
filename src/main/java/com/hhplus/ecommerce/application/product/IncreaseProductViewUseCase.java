package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import com.hhplus.ecommerce.infrastructure.product.memory.ProductStatisticsTable;
import org.springframework.stereotype.Service;

@Service
public class IncreaseProductViewUseCase {

    private final ProductStatisticsRepository productStatisticsRepository;
    private final ProductStatisticsTable productStatisticsTable;

    public IncreaseProductViewUseCase(ProductStatisticsRepository productStatisticsRepository,
                                     ProductStatisticsTable productStatisticsTable) {
        this.productStatisticsRepository = productStatisticsRepository;
        this.productStatisticsTable = productStatisticsTable;
    }

    public void execute(long productId) {
        // 통계가 없으면 생성하고, 있으면 조회수 증가
        ProductStatisticsEntity statistics = productStatisticsTable.getOrCreateDefault(productId);
        ProductStatisticsEntity updated = statistics.increaseViewCount();
        productStatisticsRepository.save(updated);
    }
}
