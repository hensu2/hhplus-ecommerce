package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.cart.CartRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.cart.res.CartItemResponse;
import com.hhplus.ecommerce.presentation.cart.res.CartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCartUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    public CartResponse execute(Long userId) {
        // 1. 사용자의 장바구니 조회
        List<CartEntity> carts = cartRepository.findByUserId(userId);

        // 2. 각 장바구니 아이템의 상세 정보 조회
        List<CartItemResponse> items = new ArrayList<>();
        int totalAmount = 0;

        for (CartEntity cart : carts) {
            ProductEntity product = productRepository.findById(cart.productId())
                .orElse(null);
            ProductOptionEntity option = productOptionRepository.findById(cart.productOptionId())
                .orElse(null);

            if (product == null || option == null) {
                continue; // 상품이나 옵션이 삭제된 경우 건너뛰기
            }

            int unitPrice = (int) (product.price() + option.additionalPrice());
            int itemTotalPrice = unitPrice * cart.quantity();

            items.add(new CartItemResponse(
                cart.id(),
                product.id(),
                product.productName(),
                option.id(),
                option.optionType(),
                cart.quantity(),
                unitPrice,
                itemTotalPrice,
                (int) option.stock()
            ));

            totalAmount += itemTotalPrice;
        }

        return new CartResponse(items, totalAmount);
    }
}
