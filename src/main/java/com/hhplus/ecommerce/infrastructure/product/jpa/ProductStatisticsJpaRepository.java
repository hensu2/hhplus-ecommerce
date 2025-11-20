package com.hhplus.ecommerce.infrastructure.product.jpa;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStatisticsJpaRepository extends JpaRepository<ProductStatisticsEntity, Long> {
}
