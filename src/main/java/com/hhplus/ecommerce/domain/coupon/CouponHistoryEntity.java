package com.hhplus.ecommerce.domain.coupon;

import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "coupon_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private Long issuedAt;

    @Column(name = "used_at")
    private Long usedAt;

    private CouponHistoryEntity(Long id, Long userId, Long couponId, CouponStatus status, Long issuedAt, Long usedAt) {
        this.id = id;
        this.userId = userId;
        this.couponId = couponId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
    }

    public static CouponHistoryEntity create(Long userId, Long couponId) {
        return new CouponHistoryEntity(null, userId, couponId, CouponStatus.ISSUED, System.currentTimeMillis(), null);
    }

    public static CouponHistoryEntity createForTest(Long id, Long userId, Long couponId, CouponStatus status, Long issuedAt, Long usedAt) {
        return new CouponHistoryEntity(id, userId, couponId, status, issuedAt, usedAt);
    }

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

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }

    public void use() {
        this.status = CouponStatus.USED;
        this.usedAt = System.currentTimeMillis();
    }
}
