package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.cart.CartRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddToCartUseCase {

    private final CartRepository cartRepository;
    private final ProductOptionRepository productOptionRepository;

    @Transactional
    public CartEntity execute(Long userId, Long productId, Long productOptionId, Integer quantity) {
        // Validate product option and check stock
        ProductOptionEntity productOption = productOptionRepository.getOrThrow(productOptionId);

        if (productOption.getStock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + productOption.getStock());
        }

        CartEntity cart = CartEntity.create(userId, productId, productOptionId, quantity);
        return cartRepository.save(cart);
    }
}
