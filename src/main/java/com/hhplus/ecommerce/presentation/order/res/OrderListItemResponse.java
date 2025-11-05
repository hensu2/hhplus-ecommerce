package com.hhplus.ecommerce.presentation.order.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItemResponse {
    private Long orderId;
    private String status;
    private Integer totalAmount;
    private Integer finalAmount;
    private Integer itemCount;
    private String orderedAt;
}
