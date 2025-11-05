package com.hhplus.ecommerce.presentation.cart.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    private Long productId;
    private Long optionId;
    private Integer quantity;
}
