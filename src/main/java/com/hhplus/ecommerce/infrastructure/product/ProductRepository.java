package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.common.exception.ProductNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<ProductEntity> findAll();
    Page<ProductEntity> findAll(Pageable pageable);
    Optional<ProductEntity> findById(Long id);

    default ProductEntity getOrThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다. ID: " + id));
    }
}
