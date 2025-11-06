package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import com.hhplus.ecommerce.presentation.product.res.PopularProductResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetPopularProductsUseCase {

    private final ProductRepository productRepository;
    private final ProductStatisticsRepository productStatisticsRepository;

    public GetPopularProductsUseCase(ProductRepository productRepository,
                                    ProductStatisticsRepository productStatisticsRepository) {
        this.productRepository = productRepository;
        this.productStatisticsRepository = productStatisticsRepository;
    }

    public List<PopularProductResponse> execute(int limit) {
        // 1. 모든 상품 통계 조회
        List<ProductStatisticsEntity> allStatistics = productStatisticsRepository.findAll();

        // 2. 인기도 점수로 정렬하고 상위 N개 선택
        return allStatistics.stream()
            .sorted(Comparator.comparingLong(ProductStatisticsEntity::getPopularityScore).reversed())
            .limit(limit)
            .map(stats -> {
                ProductEntity product = productRepository.findById(stats.productId())
                    .orElse(null);

                if (product == null) {
                    return null;
                }

                return new PopularProductResponse(
                    product.id(),
                    product.productName(),
                    (int) product.price(),
                    stats.viewCount(),
                    stats.salesCount(),
                    stats.getPopularityScore()
                );
            })
            .filter(response -> response != null)
            .collect(Collectors.toList());
    }
}
