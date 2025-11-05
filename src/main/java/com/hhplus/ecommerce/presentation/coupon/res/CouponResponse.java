package com.hhplus.ecommerce.presentation.coupon.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private Long couponId;
    private String couponName;
    private String discountType;
    private Integer discountAmount;
    private Integer useMinAmount;
    private Integer useMaxAmount;
    private String validFrom;
    private String validUntil;
    private String status;
    private String issuedAt;
    private String usedAt;
}
