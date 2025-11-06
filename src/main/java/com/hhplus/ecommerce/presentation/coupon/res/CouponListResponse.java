package com.hhplus.ecommerce.presentation.coupon.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponListResponse {
    private Long id;
    private String couponName;
    private String discountType;
    private Integer discountAmount;
    private Integer useMinAmount;
    private Integer useMaxAmount;
    private Integer stock;
    private String validFrom;
    private String validUntil;
}
