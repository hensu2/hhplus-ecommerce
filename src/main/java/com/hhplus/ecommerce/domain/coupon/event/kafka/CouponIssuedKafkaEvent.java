package com.hhplus.ecommerce.domain.coupon.event.kafka;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.event.CouponEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 발급 Kafka 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssuedKafkaEvent extends CouponKafkaEvent {
    private Long couponHistoryId;
    private Long couponId;
    private String couponName;
    private Long userId;
    private String discountType;
    private Integer discountAmount;

    public CouponIssuedKafkaEvent(
        CouponHistoryEntity couponHistory,
        CouponEntity coupon
    ) {
        super(CouponEventType.COUPON_ISSUED);
        this.couponHistoryId = couponHistory.getId();
        this.couponId = coupon.getId();
        this.couponName = coupon.getCouponName();
        this.userId = couponHistory.getUserId();
        this.discountType = coupon.getDiscountType().name();
        this.discountAmount = coupon.getDiscountAmount();
    }
}