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

    /**
     * 조회수를 원자적으로 증가시킵니다.
     * ConcurrentHashMap의 compute 메서드를 사용하여 동시성을 제어합니다.
     */
    public ProductStatisticsEntity incrementViewCount(long productId) {
        return table.compute(productId, (id, existing) -> {
            if (existing == null) {
                return new ProductStatisticsEntity(id, 1, 0, System.currentTimeMillis());
            }
            return existing.increaseViewCount();
        });
    }

    /**
     * 판매량을 원자적으로 증가시킵니다.
     * ConcurrentHashMap의 compute 메서드를 사용하여 동시성을 제어합니다.
     */
    public ProductStatisticsEntity incrementSalesCount(long productId, int quantity) {
        return table.compute(productId, (id, existing) -> {
            if (existing == null) {
                return new ProductStatisticsEntity(id, 0, quantity, System.currentTimeMillis());
            }
            return existing.increaseSalesCount(quantity);
        });
    }
}
