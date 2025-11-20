package com.hhplus.ecommerce.infrastructure.product.jpa;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
}
