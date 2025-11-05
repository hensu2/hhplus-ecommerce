package com.hhplus.ecommerce.presentation.cart.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private List<CartItemResponse> items;
    private Integer totalAmount;
}
