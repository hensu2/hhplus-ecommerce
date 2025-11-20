package com.hhplus.ecommerce.infrastructure.cart.jpa;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartJpaRepository extends JpaRepository<CartEntity, Long> {
    List<CartEntity> findByUserId(Long userId);
}
