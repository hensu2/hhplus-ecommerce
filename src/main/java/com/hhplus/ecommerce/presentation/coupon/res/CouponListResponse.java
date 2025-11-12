package com.hhplus.ecommerce.presentation.coupon.res;

public record CouponListResponse(
    Long id,
    String couponName,
    String discountType,
    Integer discountAmount,
    Integer useMinAmount,
    Integer useMaxAmount,
    Integer stock,
    String validFrom,
    String validUntil
) {
}
