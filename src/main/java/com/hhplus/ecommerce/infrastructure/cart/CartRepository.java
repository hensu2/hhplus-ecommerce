package com.hhplus.ecommerce.infrastructure.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;

import java.util.List;
import java.util.Optional;

public interface CartRepository {
    List<CartEntity> findByUserId(Long userId);
    Optional<CartEntity> findById(Long id);
    CartEntity save(CartEntity cart);
    void deleteById(Long id);

    default CartEntity getOrThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));
    }
}
