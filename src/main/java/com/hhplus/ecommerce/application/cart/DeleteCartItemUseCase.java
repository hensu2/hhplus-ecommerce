package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.infrastructure.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteCartItemUseCase {

    private final CartRepository cartRepository;

    public void execute(Long cartId) {
        // 장바구니 아이템 존재 여부 확인
        cartRepository.findById(cartId)
            .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));

        // 삭제
        cartRepository.delete(cartId);
    }
}
