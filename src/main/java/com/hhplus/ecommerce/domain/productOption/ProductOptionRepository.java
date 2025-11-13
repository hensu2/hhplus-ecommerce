package com.hhplus.ecommerce.domain.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {
    List<ProductOptionEntity> findByProductId(Long productId);
    Optional<ProductOptionEntity> findById(Long id);
    ProductOptionEntity save(ProductOptionEntity productOption);
}