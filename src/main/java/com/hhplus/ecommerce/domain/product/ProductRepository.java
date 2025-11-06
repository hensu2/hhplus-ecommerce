package com.hhplus.ecommerce.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<ProductEntity> findAll();
    Optional<ProductEntity> findById(Long id);
}
