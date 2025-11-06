package com.hhplus.ecommerce.domain.coupon;

import com.hhplus.ecommerce.presentation.coupon.res.CouponListResponse;

import java.time.Instant;
import java.time.ZoneId;

public record CouponEntity(
    long id,
    String couponName,
    DiscountType discountType,
    int discountAmount,
    int useMinAmount,
    int useMaxAmount,
    int stock,
    long validFrom,
    long validUntil,
    long createdAt,
    long updatedAt
) {
    public CouponListResponse toCouponListResponse() {
        return new CouponListResponse(
            this.id,
            this.couponName,
            this.discountType.name(),
            this.discountAmount,
            this.useMinAmount,
            this.useMaxAmount,
            this.stock,
            formatTimestamp(this.validFrom),
            formatTimestamp(this.validUntil)
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
