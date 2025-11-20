package com.hhplus.ecommerce.infrastructure.productOption.jpa;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOptionEntity, Long> {

    List<ProductOptionEntity> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductOptionEntity p WHERE p.id = :id")
    Optional<ProductOptionEntity> findByIdWithLock(@Param("id") Long id);
}
