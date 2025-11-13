package com.hhplus.ecommerce.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    ProductEntity save(ProductEntity product);
    Optional<ProductEntity> findById(Long id);
    List<ProductEntity> findAll();
    List<ProductEntity> findByProductNameContaining(String keyword);
    List<ProductEntity> findByCreatedUserId(Long userId);
}
