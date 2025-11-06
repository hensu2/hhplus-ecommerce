package com.hhplus.ecommerce.infrastructure.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;

import java.util.List;

public interface CouponRepository {
    List<CouponEntity> findAll();
}
