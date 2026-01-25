package com.hhplus.ecommerce.domain.coupon.event.kafka;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.event.CouponEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 사용 Kafka 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsedKafkaEvent extends CouponKafkaEvent {
    private Long couponHistoryId;
    private Long couponId;
    private String couponName;
    private Long userId;
    private String discountType;
    private Integer discountAmount;
    private Long orderId;

    public CouponUsedKafkaEvent(
        CouponHistoryEntity couponHistory,
        CouponEntity coupon,
        Long orderId
    ) {
        super(CouponEventType.COUPON_USED);
        this.couponHistoryId = couponHistory.getId();
        this.couponId = coupon.getId();
        this.couponName = coupon.getCouponName();
        this.userId = couponHistory.getUserId();
        this.discountType = coupon.getDiscountType().name();
        this.discountAmount = coupon.getDiscountAmount();
        this.orderId = orderId;
    }
}