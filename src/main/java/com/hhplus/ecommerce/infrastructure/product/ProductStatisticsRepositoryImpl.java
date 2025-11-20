package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.jpa.ProductStatisticsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductStatisticsRepositoryImpl implements ProductStatisticsRepository {

    private final ProductStatisticsJpaRepository productStatisticsJpaRepository;

    @Override
    public ProductStatisticsEntity save(ProductStatisticsEntity statistics) {
        return productStatisticsJpaRepository.save(statistics);
    }

    @Override
    public Optional<ProductStatisticsEntity> findByProductId(long productId) {
        return productStatisticsJpaRepository.findById(productId);
    }

    @Override
    public List<ProductStatisticsEntity> findAll() {
        return productStatisticsJpaRepository.findAll();
    }
}
