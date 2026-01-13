package com.hhplus.ecommerce.application.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CouponIssueRequest {
    private Long userId;
    private Long couponId;
    private Long requestedAt;
}