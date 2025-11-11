package com.hhplus.ecommerce.presentation.coupon.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponsResponse {
    private List<CouponListResponse> coupons;
}
