package com.hhplus.ecommerce.infrastructure.product.memory;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProductStatisticsTable {
    private final ConcurrentHashMap<Long, ProductStatisticsEntity> table = new ConcurrentHashMap<>();

    public ProductStatisticsEntity save(ProductStatisticsEntity statistics) {
        table.put(statistics.productId(), statistics);
        return statistics;
    }

    public Optional<ProductStatisticsEntity> findByProductId(long productId) {
        return Optional.ofNullable(table.get(productId));
    }

    public List<ProductStatisticsEntity> findAll() {
        return new ArrayList<>(table.values());
    }

    public ProductStatisticsEntity getOrCreateDefault(long productId) {
        return table.computeIfAbsent(productId, id ->
            new ProductStatisticsEntity(id, 0, 0, System.currentTimeMillis())
        );
    }
}
