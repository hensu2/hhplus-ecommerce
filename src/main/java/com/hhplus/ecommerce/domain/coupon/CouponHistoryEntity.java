package com.hhplus.ecommerce.domain.coupon;

import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "coupon_history")
public class CouponHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private Long issuedAt;

    @Column(name = "used_at")
    private Long usedAt;

    public IssueCouponResponse toIssueCouponResponse(CouponEntity coupon) {
        return new IssueCouponResponse(
            this.id,
            this.couponId,
            coupon.getCouponName(),
            coupon.getDiscountType().name(),
            coupon.getDiscountAmount(),
            formatTimestamp(coupon.getValidFrom()),
            formatTimestamp(coupon.getValidUntil()),
            this.status.name(),
            formatTimestamp(this.issuedAt)
        );
    }

    private String formatTimestamp(Long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }

    public void setStatus(CouponStatus status) {
        this.status = status;
    }

    public void setUsedAt(Long usedAt) {
        this.usedAt = usedAt;
    }
}
