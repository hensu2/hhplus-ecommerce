package com.hhplus.ecommerce.presentation.coupon.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;

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
    public IssueCouponResponse(CouponHistoryEntity history, CouponEntity coupon) {
        this(
            history.getId(),
            history.getCouponId(),
            coupon.getCouponName(),
            coupon.getDiscountType().name(),
            coupon.getDiscountAmount(),
            DateTimeUtils.toLocalDateTime(coupon.getValidFrom()),
            DateTimeUtils.toLocalDateTime(coupon.getValidUntil()),
            history.getStatus().name(),
            DateTimeUtils.toLocalDateTime(history.getIssuedAt())
        );
    }
}
