package com.hhplus.ecommerce.infrastructure.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    List<CouponEntity> findAll();
    Optional<CouponEntity> findById(long couponId);
    CouponEntity save(CouponEntity coupon);
    CouponHistoryEntity saveHistory(CouponHistoryEntity history);
    Optional<CouponHistoryEntity> findHistoryByUserIdAndCouponId(long userId, long couponId);
    List<CouponHistoryEntity> findHistoriesByUserId(long userId);
    CouponEntity decreaseStock(long couponId);

    /**
     * 배치 삽입 (DB 동기화용)
     */
    List<CouponHistoryEntity> saveAllHistories(List<CouponHistoryEntity> histories);

    default CouponEntity getOrThrow(long couponId) {
        return findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
    }
}
