package com.hhplus.ecommerce.presentation.coupon.res;

public record IssueCouponResponse(
    Long id,
    Long couponId,
    String couponName,
    String discountType,
    Integer discountAmount,
    String validFrom,
    String validUntil,
    String status,
    String issuedAt
) {
}
