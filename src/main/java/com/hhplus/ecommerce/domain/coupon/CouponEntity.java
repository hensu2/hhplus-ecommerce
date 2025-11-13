package com.hhplus.ecommerce.domain.coupon;

import com.hhplus.ecommerce.presentation.coupon.res.CouponListResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "valid_from", nullable = false)
    private Long validFrom;

    @Column(name = "valid_until", nullable = false)
    private Long validUntil;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    private CouponEntity(Long id, String couponName, DiscountType discountType, Integer discountAmount,
                         Integer useMinAmount, Integer useMaxAmount, Integer stock, Long validFrom,
                         Long validUntil, Long createdAt, Long updatedAt) {
        this.id = id;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.useMinAmount = useMinAmount;
        this.useMaxAmount = useMaxAmount;
        this.stock = stock;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CouponEntity create(String couponName, DiscountType discountType, Integer discountAmount,
                                      Integer useMinAmount, Integer useMaxAmount, Integer stock,
                                      Long validFrom, Long validUntil) {
        long now = System.currentTimeMillis();
        return new CouponEntity(null, couponName, discountType, discountAmount, useMinAmount,
            useMaxAmount, stock, validFrom, validUntil, now, now);
    }

    public static CouponEntity createForTest(Long id, String couponName, DiscountType discountType,
                                             Integer discountAmount, Integer useMinAmount, Integer useMaxAmount,
                                             Integer stock, Long validFrom, Long validUntil, Long createdAt, Long updatedAt) {
        return new CouponEntity(id, couponName, discountType, discountAmount, useMinAmount,
            useMaxAmount, stock, validFrom, validUntil, createdAt, updatedAt);
    }

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

    public void decreaseStock() {
        if (this.stock <= 0) {
            throw new IllegalStateException("쿠폰 재고가 부족합니다.");
        }
        this.stock = this.stock - 1;
        this.updatedAt = System.currentTimeMillis();
    }
}
