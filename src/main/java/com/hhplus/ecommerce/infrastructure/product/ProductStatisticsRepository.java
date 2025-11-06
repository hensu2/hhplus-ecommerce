package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;

import java.util.List;
import java.util.Optional;

public interface ProductStatisticsRepository {
    ProductStatisticsEntity save(ProductStatisticsEntity statistics);
    Optional<ProductStatisticsEntity> findByProductId(long productId);
    List<ProductStatisticsEntity> findAll();
}
