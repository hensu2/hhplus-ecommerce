package com.hhplus.ecommerce.presentation.order.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
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
    private CouponInfo coupon;
    private PaymentInfo payment;
    private String orderedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponInfo {
        private String couponName;
        private Integer discountAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private Long paymentId;
        private Integer paymentAmount;
        private String paidAt;
    }
}
