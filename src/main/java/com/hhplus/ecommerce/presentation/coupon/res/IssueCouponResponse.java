package com.hhplus.ecommerce.presentation.coupon.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IssueCouponResponse {
    private Long id;
    private Long couponId;
    private String couponName;
    private String discountType;
    private Integer discountAmount;
    private String validFrom;
    private String validUntil;
    private String status;
    private String issuedAt;
}
