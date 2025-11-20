package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.infrastructure.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCartItemUseCase {

    private final CartRepository cartRepository;

    @Transactional
    public void execute(Long cartId) {
        // Verify cart exists
        cartRepository.getOrThrow(cartId);
        cartRepository.deleteById(cartId);
    }
}
