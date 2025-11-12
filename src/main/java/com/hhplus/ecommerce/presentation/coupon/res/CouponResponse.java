package com.hhplus.ecommerce.presentation.coupon.res;

public record CouponResponse(
    Long id,
    Long couponId,
    String couponName,
    String discountType,
    Integer discountAmount,
    Integer useMinAmount,
    Integer useMaxAmount,
    String validFrom,
    String validUntil,
    String status,
    String issuedAt,
    String usedAt
) {
}
