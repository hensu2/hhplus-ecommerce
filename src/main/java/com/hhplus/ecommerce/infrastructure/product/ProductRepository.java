package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<ProductEntity> findAll();
    Optional<ProductEntity> findById(Long id);
}