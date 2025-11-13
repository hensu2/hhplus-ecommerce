package com.hhplus.ecommerce.infrastructure.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartEntity, Long> {
    List<CartEntity> findByUserId(Long userId);
    Optional<CartEntity> findByUserIdAndProductOptionId(Long userId, Long productOptionId);
    void deleteByUserId(Long userId);
}
