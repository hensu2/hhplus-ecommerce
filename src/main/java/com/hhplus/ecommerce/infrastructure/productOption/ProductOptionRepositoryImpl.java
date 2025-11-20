package com.hhplus.ecommerce.infrastructure.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.infrastructure.productOption.jpa.ProductOptionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class ProductOptionRepositoryImpl implements ProductOptionRepository {

    private final ProductOptionJpaRepository productOptionJpaRepository;

    // 동시성 제어를 위한 ConcurrentHashMap (재고 차감용)
    private final ConcurrentHashMap<Long, Object> stockLocks = new ConcurrentHashMap<>();

    @Override
    public List<ProductOptionEntity> findByProductId(Long productId) {
        return productOptionJpaRepository.findByProductId(productId);
    }

    @Override
    public Optional<ProductOptionEntity> findById(Long id) {
        return productOptionJpaRepository.findById(id);
    }

    @Override
    public ProductOptionEntity save(ProductOptionEntity productOption) {
        return productOptionJpaRepository.save(productOption);
    }

    @Override
    @Transactional
    public ProductOptionEntity decreaseStock(Long optionId, long quantity) {
        // ConcurrentHashMap을 사용한 동시성 제어
        Object lock = stockLocks.computeIfAbsent(optionId, k -> new Object());

        synchronized (lock) {
            ProductOptionEntity productOption = productOptionJpaRepository.findByIdWithLock(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

            productOption.updateStock(StockUpdateType.DECREASE, (int) quantity);
            return productOptionJpaRepository.save(productOption);
        }
    }
}
