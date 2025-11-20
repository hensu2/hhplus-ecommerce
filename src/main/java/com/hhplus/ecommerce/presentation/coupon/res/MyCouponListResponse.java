package com.hhplus.ecommerce.presentation.coupon.res;

import java.util.List;

public record MyCouponListResponse(
        List<CouponResponse> coupons
) {
}
