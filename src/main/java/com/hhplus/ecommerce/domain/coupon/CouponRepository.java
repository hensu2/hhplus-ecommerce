package com.hhplus.ecommerce.domain.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    List<CouponEntity> findAll();
    Optional<CouponEntity> findById(long couponId);
    CouponEntity save(CouponEntity coupon);
    CouponHistoryEntity saveHistory(CouponHistoryEntity history);
    Optional<CouponHistoryEntity> findHistoryById(long couponHistoryId);
    Optional<CouponHistoryEntity> findHistoryByUserIdAndCouponId(long userId, long couponId);
    List<CouponHistoryEntity> findHistoriesByUserId(long userId);

    /**
     * 쿠폰 재고를 원자적으로 차감합니다.
     */
    CouponEntity decreaseStock(long couponId);
}
