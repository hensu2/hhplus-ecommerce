package com.hhplus.ecommerce.infrastructure.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.domain.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpaRepository;

    @Override
    public CartEntity save(CartEntity cart) {
        return cartJpaRepository.save(cart);
    }

    @Override
    public Optional<CartEntity> findById(Long id) {
        return cartJpaRepository.findById(id);
    }

    @Override
    public List<CartEntity> findByUserId(Long userId) {
        return cartJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<CartEntity> findByUserIdAndProductOptionId(Long userId, Long productOptionId) {
        return cartJpaRepository.findByUserIdAndProductOptionId(userId, productOptionId);
    }

    @Override
    public void delete(Long id) {
        cartJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        cartJpaRepository.deleteByUserId(userId);
    }
}
