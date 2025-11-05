package com.hhplus.ecommerce.presentation.order.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Long optionId;
    private String optionType;
    private Integer quantity;
    private Integer unitPrice;
    private Integer totalPrice;
}
