package com.hhplus.ecommerce.infrastructure.productOption.memory;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ProductOptionTable {
    private final ConcurrentHashMap<Long, ProductOptionEntity> table = new ConcurrentHashMap<>();
    private long cursor = 1;

    @PostConstruct
    public void init() {
        long now = System.currentTimeMillis();

        // 상품 1번(노트북)의 옵션
        table.put(1L, new ProductOptionEntity(1L, 1L, "색상:블랙", 0L, 100L, now, now));
        table.put(2L, new ProductOptionEntity(2L, 1L, "색상:실버", 10000L, 50L, now, now));

        // 상품 2번(키보드)의 옵션
        table.put(3L, new ProductOptionEntity(3L, 2L, "축:청축", 0L, 80L, now, now));
        table.put(4L, new ProductOptionEntity(4L, 2L, "축:적축", 0L, 70L, now, now));

        cursor = 5;
    }

    public List<ProductOptionEntity> findByProductId(Long productId) {
        return table.values().stream()
            .filter(option -> option.productId() == productId)
            .toList();
    }

    public ProductOptionEntity findById(Long id) {
        return table.get(id);
    }

    public ProductOptionEntity save(ProductOptionEntity productOption) {
        table.put(productOption.id(), productOption);
        return productOption;
    }
}