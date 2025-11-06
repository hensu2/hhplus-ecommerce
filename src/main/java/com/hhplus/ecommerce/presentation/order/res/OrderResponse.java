package com.hhplus.ecommerce.presentation.order.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long userId;
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private String status;
    private List<OrderItemResponse> items;
    private String createdAt;
}
