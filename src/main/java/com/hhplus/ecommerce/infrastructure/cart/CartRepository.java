package com.hhplus.ecommerce.infrastructure.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;

import java.util.List;
import java.util.Optional;

public interface CartRepository {
    CartEntity save(CartEntity cart);
    Optional<CartEntity> findById(Long id);
    List<CartEntity> findByUserId(Long userId);
    Optional<CartEntity> findByUserIdAndProductOptionId(Long userId, Long productOptionId);
    void delete(Long id);
    void deleteByUserId(Long userId);
}
