package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.cart.CartRepository;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.cart.res.AddCartItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddToCartUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public AddCartItemResponse execute(Long userId, Long productId, Long productOptionId, Integer quantity) {
        // 1. 상품 및 옵션 검증
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        ProductOptionEntity option = productOptionRepository.findById(productOptionId)
            .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

        // 2. 재고 확인
        if (option.getStock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        // 3. 동일 상품 옵션이 이미 장바구니에 있는지 확인
        Optional<CartEntity> existingCart = cartRepository.findByUserIdAndProductOptionId(userId, productOptionId);

        CartEntity savedCart;
        if (existingCart.isPresent()) {
            // 기존 장바구니 아이템의 수량 증가
            CartEntity existing = existingCart.get();
            int newQuantity = existing.getQuantity() + quantity;

            if (option.getStock() < newQuantity) {
                throw new IllegalArgumentException("재고가 부족합니다.");
            }

            existing.updateQuantity(newQuantity);
            savedCart = cartRepository.save(existing);
        } else {
            // 새로운 장바구니 아이템 생성
            CartEntity newCart = CartEntity.create(userId, productId, productOptionId, quantity);
            savedCart = cartRepository.save(newCart);
        }

        // 4. 응답 생성
        int unitPrice = (int) (product.getPrice() + option.getAdditionalPrice());
        int totalPrice = unitPrice * savedCart.getQuantity();

        return new AddCartItemResponse(
            savedCart.getId(),
            product.getId(),
            product.getProductName(),
            option.getId(),
            option.getOptionType(),
            savedCart.getQuantity(),
            unitPrice,
            totalPrice,
            FORMATTER.format(Instant.ofEpochMilli(savedCart.getCreatedAt()))
        );
    }
}
