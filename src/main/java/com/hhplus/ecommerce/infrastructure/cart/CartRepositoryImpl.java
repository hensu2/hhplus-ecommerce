package com.hhplus.ecommerce.infrastructure.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.infrastructure.cart.memory.CartTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartTable cartTable;

    @Override
    public CartEntity save(CartEntity cart) {
        return cartTable.save(cart);
    }

    @Override
    public Optional<CartEntity> findById(Long id) {
        return cartTable.findById(id);
    }

    @Override
    public List<CartEntity> findByUserId(Long userId) {
        return cartTable.findByUserId(userId);
    }

    @Override
    public Optional<CartEntity> findByUserIdAndProductOptionId(Long userId, Long productOptionId) {
        return cartTable.findByUserIdAndProductOptionId(userId, productOptionId);
    }

    @Override
    public void delete(Long id) {
        cartTable.delete(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        cartTable.deleteByUserId(userId);
    }
}
