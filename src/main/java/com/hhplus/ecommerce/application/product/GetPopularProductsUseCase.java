package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPopularProductsUseCase {

    private final ProductStatisticsRepository productStatisticsRepository;

    public List<ProductStatisticsEntity> execute(int limit) {
        List<ProductStatisticsEntity> allStatistics = productStatisticsRepository.findAll();

        return allStatistics.stream()
            .sorted(Comparator.comparingLong(ProductStatisticsEntity::getPopularityScore).reversed())
            .limit(limit)
            .toList();
    }
}
