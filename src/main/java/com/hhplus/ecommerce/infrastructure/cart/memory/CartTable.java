package com.hhplus.ecommerce.infrastructure.cart.memory;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CartTable {
    private final ConcurrentHashMap<Long, CartEntity> table = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public CartEntity save(CartEntity cart) {
        if (cart.id() == 0L) {
            Long newId = idGenerator.getAndIncrement();
            CartEntity newCart = cart.withId(newId);
            table.put(newId, newCart);
            return newCart;
        } else {
            table.put(cart.id(), cart);
            return cart;
        }
    }

    public Optional<CartEntity> findById(Long id) {
        return Optional.ofNullable(table.get(id));
    }

    public List<CartEntity> findByUserId(Long userId) {
        return table.values().stream()
            .filter(cart -> cart.userId().equals(userId))
            .toList();
    }

    public Optional<CartEntity> findByUserIdAndProductOptionId(Long userId, Long productOptionId) {
        return table.values().stream()
            .filter(cart -> cart.userId().equals(userId) && cart.productOptionId().equals(productOptionId))
            .findFirst();
    }

    public void delete(Long id) {
        table.remove(id);
    }

    public void deleteByUserId(Long userId) {
        table.values().removeIf(cart -> cart.userId().equals(userId));
    }
}
