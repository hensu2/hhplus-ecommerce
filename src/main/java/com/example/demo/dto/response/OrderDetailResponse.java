package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private Long orderId;
    private Long userId;
    private String status;
    private List<OrderItemResponse> items;
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer pointDiscount;
    private Integer finalAmount;
    private OrderCouponResponse coupon;
    private OrderPaymentResponse payment;
    private LocalDateTime orderedAt;
}
