package com.hhplus.ecommerce.domain.coupon;

import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;

import java.time.Instant;
import java.time.ZoneId;

public record CouponHistoryEntity(
    long id,
    long userId,
    long couponId,
    CouponStatus status,
    long issuedAt,
    Long usedAt
) {
    public IssueCouponResponse toIssueCouponResponse(CouponEntity coupon) {
        return new IssueCouponResponse(
            this.id,
            this.couponId,
            coupon.couponName(),
            coupon.discountType().name(),
            coupon.discountAmount(),
            formatTimestamp(coupon.validFrom()),
            formatTimestamp(coupon.validUntil()),
            this.status.name(),
            formatTimestamp(this.issuedAt)
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}