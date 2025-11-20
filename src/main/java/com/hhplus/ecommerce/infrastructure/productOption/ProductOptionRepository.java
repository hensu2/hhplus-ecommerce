package com.hhplus.ecommerce.infrastructure.productOption;

import com.hhplus.ecommerce.common.exception.ProductOptionNotFoundException;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {
    List<ProductOptionEntity> findByProductId(Long productId);
    Optional<ProductOptionEntity> findById(Long id);
    ProductOptionEntity save(ProductOptionEntity productOption);
    ProductOptionEntity decreaseStock(Long optionId, long quantity);

    default ProductOptionEntity getOrThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new ProductOptionNotFoundException("상품 옵션을 찾을 수 없습니다. ID: " + id));
    }
}