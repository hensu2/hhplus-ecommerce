package com.hhplus.ecommerce.domain.coupon.event;

/**
 * 쿠폰 이벤트 타입
 */
public enum CouponEventType {
    COUPON_ISSUED,    // 쿠폰 발급
    COUPON_USED,      // 쿠폰 사용
    COUPON_EXPIRED    // 쿠폰 만료
}