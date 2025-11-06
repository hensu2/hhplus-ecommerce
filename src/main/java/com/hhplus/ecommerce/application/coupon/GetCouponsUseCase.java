package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.CouponListResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetCouponsUseCase {

    private final CouponRepository couponRepository;

    public GetCouponsUseCase(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public List<CouponListResponse> execute() {
        List<CouponEntity> coupons = couponRepository.findAll();

        return coupons.stream()
            .map(CouponEntity::toCouponListResponse)
            .collect(Collectors.toList());
    }
}
