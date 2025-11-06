package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.memory.ProductStatisticsTable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductStatisticsRepositoryImpl implements ProductStatisticsRepository {

    private final ProductStatisticsTable productStatisticsTable;

    public ProductStatisticsRepositoryImpl(ProductStatisticsTable productStatisticsTable) {
        this.productStatisticsTable = productStatisticsTable;
    }

    @Override
    public ProductStatisticsEntity save(ProductStatisticsEntity statistics) {
        return productStatisticsTable.save(statistics);
    }

    @Override
    public Optional<ProductStatisticsEntity> findByProductId(long productId) {
        return productStatisticsTable.findByProductId(productId);
    }

    @Override
    public List<ProductStatisticsEntity> findAll() {
        return productStatisticsTable.findAll();
    }
}
