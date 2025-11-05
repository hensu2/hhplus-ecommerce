package com.hhplus.ecommerce.presentation.cart.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long optionId;
    private String optionType;
    private Integer quantity;
    private Integer unitPrice;
    private Integer totalPrice;
    private Integer stock;
}
