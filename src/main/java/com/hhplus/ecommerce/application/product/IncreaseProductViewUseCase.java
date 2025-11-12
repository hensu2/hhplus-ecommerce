package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import com.hhplus.ecommerce.infrastructure.product.memory.ProductStatisticsTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncreaseProductViewUseCase {

    private final ProductStatisticsRepository productStatisticsRepository;
    private final ProductStatisticsTable productStatisticsTable;

    public void execute(long productId) {
        // 조회수를 원자적으로 증가
        ProductStatisticsEntity updated = productStatisticsTable.incrementViewCount(productId);
        productStatisticsRepository.save(updated);
    }
}
