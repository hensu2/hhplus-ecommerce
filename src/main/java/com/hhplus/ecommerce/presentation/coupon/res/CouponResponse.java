package com.hhplus.ecommerce.presentation.coupon.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;

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
    public CouponResponse(CouponHistoryEntity history, CouponEntity coupon) {
        this(
            history.getId(),
            history.getCouponId(),
            coupon.getCouponName(),
            coupon.getDiscountType().name(),
            coupon.getDiscountAmount(),
            coupon.getUseMinAmount(),
            coupon.getUseMaxAmount(),
            DateTimeUtils.toLocalDateTime(coupon.getValidFrom()),
            DateTimeUtils.toLocalDateTime(coupon.getValidUntil()),
            history.getStatus().name(),
            DateTimeUtils.toLocalDateTime(history.getIssuedAt()),
            history.getUsedAt() != null ? DateTimeUtils.toLocalDateTime(history.getUsedAt()) : null
        );
    }
}
