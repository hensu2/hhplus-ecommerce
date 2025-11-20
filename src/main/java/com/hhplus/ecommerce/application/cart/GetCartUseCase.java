package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.infrastructure.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCartUseCase {

    private final CartRepository cartRepository;

    public List<CartEntity> execute(Long userId) {
        return cartRepository.findByUserId(userId);
    }
}
