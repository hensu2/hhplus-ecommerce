package com.hhplus.ecommerce.domain.coupon;

import com.hhplus.ecommerce.presentation.coupon.res.CouponListResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.ZoneId;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "coupons")
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_name", nullable = false)
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "use_min_amount", nullable = false)
    private Integer useMinAmount;

    @Column(name = "use_max_amount", nullable = false)
    private Integer useMaxAmount;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "valid_from", nullable = false)
    private Long validFrom;

    @Column(name = "valid_until", nullable = false)
    private Long validUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

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

    private String formatTimestamp(Long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }

    public CouponEntity decreaseStock() {
        if (this.stock <= 0) {
            throw new IllegalStateException("쿠폰 재고가 부족합니다.");
        }
        this.stock = this.stock - 1;
        this.updatedAt = System.currentTimeMillis();
        return this;
    }
}
