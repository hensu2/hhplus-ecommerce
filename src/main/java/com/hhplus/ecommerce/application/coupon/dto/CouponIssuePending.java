package com.hhplus.ecommerce.application.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CouponIssuePending {
    private Long historyId;
    private Long userId;
    private Long couponId;
    private String status;
    private Long issuedAt;
}