package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProductTable {
    private final ConcurrentHashMap<Long, ProductEntity> table = new ConcurrentHashMap<>();
    private long cursor = 6;

    public ProductTable() {
        // 초기 데이터
        long timestamp = System.currentTimeMillis();
        table.put(1L, new ProductEntity(1L, 1L, "노트북", "고성능 노트북", 890000L, timestamp, timestamp));
        table.put(2L, new ProductEntity(2L, 1L, "키보드", "기계식 키보드", 120000L, timestamp, timestamp));
        table.put(3L, new ProductEntity(3L, 1L, "마우스", "게이밍 마우스", 85000L, timestamp, timestamp));
        table.put(4L, new ProductEntity(4L, 1L, "모니터", "27인치 모니터", 350000L, timestamp, timestamp));
        table.put(5L, new ProductEntity(5L, 1L, "헤드셋", "무선 헤드셋", 150000L, timestamp, timestamp));
    }

    public List<ProductEntity> findAll() {
        return List.copyOf(table.values());
    }

    public Optional<ProductEntity> findById(Long id) {
        return Optional.ofNullable(table.get(id));
    }
}