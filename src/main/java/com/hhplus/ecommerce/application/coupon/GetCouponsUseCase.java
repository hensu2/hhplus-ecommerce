package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCouponsUseCase {

    private final CouponRepository couponRepository;

    public List<CouponEntity> execute() {
        return couponRepository.findAll();
    }
}
