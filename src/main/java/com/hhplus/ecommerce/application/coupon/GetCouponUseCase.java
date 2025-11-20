package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCouponUseCase {

    private final CouponRepository couponRepository;

    public CouponEntity execute(long couponId) {
        return couponRepository.getOrThrow(couponId);
    }
}