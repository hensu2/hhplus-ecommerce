package com.hhplus.ecommerce.presentation.order.res;

import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        Long userId,
        String status,
        List<OrderItemResponse> items,
        Integer totalAmount,
        Integer discountAmount,
        Integer pointDiscount,
        Integer finalAmount,
        CouponInfo coupon,
        PaymentInfo payment,
        String orderedAt
) {
    public record CouponInfo(
            String couponName,
            Integer discountAmount
    ) {
    }

    public record PaymentInfo(
            Long paymentId,
            Integer paymentAmount,
            String paidAt
    ) {
    }
}
