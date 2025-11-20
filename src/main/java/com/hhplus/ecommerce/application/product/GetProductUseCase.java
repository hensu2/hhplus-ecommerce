package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductRepository productRepository;
    private final ProductStatisticsRepository productStatisticsRepository;

    public ProductEntity execute(Long productId) {
        ProductEntity.validateProductId(productId);

        ProductEntity product = productRepository.getOrThrow(productId);

        ProductStatisticsEntity statistics = productStatisticsRepository.findByProductId(productId)
                .orElse(new ProductStatisticsEntity(productId, 0L, 0L, System.currentTimeMillis()));

        statistics.increaseViewCount();
        productStatisticsRepository.save(statistics);

        return product;
    }
}
