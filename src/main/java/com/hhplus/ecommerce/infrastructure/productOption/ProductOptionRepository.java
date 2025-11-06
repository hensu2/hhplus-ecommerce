package com.hhplus.ecommerce.infrastructure.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {
    List<ProductOptionEntity> findByProductId(Long productId);
    Optional<ProductOptionEntity> findById(Long id);
    ProductOptionEntity save(ProductOptionEntity productOption);

    /**
     * 재고를 원자적으로 차감합니다.
     */
    ProductOptionEntity decreaseStock(Long optionId, long quantity);
}