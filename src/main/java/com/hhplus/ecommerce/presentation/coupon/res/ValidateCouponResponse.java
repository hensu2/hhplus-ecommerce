package com.hhplus.ecommerce.presentation.coupon.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCouponResponse {
    private Boolean valid;
    private Integer discountAmount;
    private Integer finalAmount;
    private String message;
}
