package com.hhplus.ecommerce.domain.coupon.event.kafka;

import com.hhplus.ecommerce.domain.coupon.event.CouponEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 Kafka 이벤트 기본 클래스
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class CouponKafkaEvent {
    private CouponEventType eventType;
    private Long timestamp;

    public CouponKafkaEvent(CouponEventType eventType) {
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }
}