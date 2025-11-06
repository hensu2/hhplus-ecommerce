package com.hhplus.ecommerce.infrastructure.product;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ProductTable {
    private final ConcurrentHashMap<Long, Product> table = new ConcurrentHashMap<>();
    private long cursor = 6;

    public ProductTable() {
        // 초기 데이터
        long timestamp = System.currentTimeMillis();
        table.put(1L, new Product(1L, 1L, "노트북", "고성능 노트북", 890000L, timestamp, timestamp));
        table.put(2L, new Product(2L, 1L, "키보드", "기계식 키보드", 120000L, timestamp, timestamp));
        table.put(3L, new Product(3L, 1L, "마우스", "게이밍 마우스", 85000L, timestamp, timestamp));
        table.put(4L, new Product(4L, 1L, "모니터", "27인치 모니터", 350000L, timestamp, timestamp));
        table.put(5L, new Product(5L, 1L, "헤드셋", "무선 헤드셋", 150000L, timestamp, timestamp));
    }

    public List<Product> findAll() {
        return List.copyOf(table.values());
    }
}