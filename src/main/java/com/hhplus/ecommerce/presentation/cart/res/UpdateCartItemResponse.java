package com.hhplus.ecommerce.presentation.cart.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemResponse {
    private Long id;
    private Long productId;
    private Integer quantity;
    private Integer totalPrice;
    private String updatedAt;
}
