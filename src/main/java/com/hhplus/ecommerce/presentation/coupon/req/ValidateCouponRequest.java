package com.hhplus.ecommerce.presentation.coupon.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCouponRequest {
    private Integer orderAmount;
}
