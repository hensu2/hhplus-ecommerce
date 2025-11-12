package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.cart.CartRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.cart.res.UpdateCartItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class UpdateCartItemUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public UpdateCartItemResponse execute(Long cartId, Integer quantity) {
        // 1. 장바구니 아이템 조회
        CartEntity cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));

        // 2. 상품 옵션 조회 및 재고 확인
        ProductOptionEntity option = productOptionRepository.findById(cart.productOptionId())
            .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

        if (option.stock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        // 3. 상품 정보 조회
        ProductEntity product = productRepository.findById(cart.productId())
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 4. 수량 업데이트
        CartEntity updatedCart = cartRepository.save(cart.updateQuantity(quantity));

        // 5. 응답 생성
        int unitPrice = (int) (product.price() + option.additionalPrice());
        int totalPrice = unitPrice * quantity;

        return new UpdateCartItemResponse(
            updatedCart.id(),
            product.id(),
            quantity,
            totalPrice,
            FORMATTER.format(Instant.ofEpochMilli(updatedCart.updatedAt()))
        );
    }
}
