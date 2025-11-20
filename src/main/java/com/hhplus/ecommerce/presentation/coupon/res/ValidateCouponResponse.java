package com.hhplus.ecommerce.presentation.coupon.res;

public record ValidateCouponResponse(
        Boolean valid,
        Integer discountAmount,
        Integer finalAmount,
        String message
) {
}
