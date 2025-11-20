package com.hhplus.ecommerce.infrastructure.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.infrastructure.cart.jpa.CartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpaRepository;

    @Override
    public List<CartEntity> findByUserId(Long userId) {
        return cartJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<CartEntity> findById(Long id) {
        return cartJpaRepository.findById(id);
    }

    @Override
    public CartEntity save(CartEntity cart) {
        return cartJpaRepository.save(cart);
    }

    @Override
    public void deleteById(Long id) {
        cartJpaRepository.deleteById(id);
    }
}
