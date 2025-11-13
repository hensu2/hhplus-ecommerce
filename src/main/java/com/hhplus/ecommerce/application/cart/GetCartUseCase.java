package com.hhplus.ecommerce.application.cart;

import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.cart.CartRepository;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
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
            ProductEntity product = productRepository.findById(cart.getProductId())
                .orElse(null);
            ProductOptionEntity option = productOptionRepository.findById(cart.getProductOptionId())
                .orElse(null);

            if (product == null || option == null) {
                continue; // 상품이나 옵션이 삭제된 경우 건너뛰기
            }

            long unitPriceLong = product.getPrice() + option.getAdditionalPrice();
            int unitPrice = (int) unitPriceLong;
            int itemTotalPrice = unitPrice * cart.getQuantity();

            items.add(new CartItemResponse(
                cart.getId(),
                product.getId(),
                product.getProductName(),
                option.getId(),
                option.getOptionType(),
                cart.getQuantity(),
                unitPrice,
                itemTotalPrice,
                option.getStock().intValue()
            ));

            totalAmount += itemTotalPrice;
        }

        return new CartResponse(items, totalAmount);
    }
}
