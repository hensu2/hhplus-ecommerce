package com.hhplus.ecommerce.infrastructure.coupon.jpa;

import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponHistoryJpaRepository extends JpaRepository<CouponHistoryEntity, Long> {

    List<CouponHistoryEntity> findByUserId(Long userId);

    List<CouponHistoryEntity> findByUserIdAndStatus(Long userId, CouponStatus status);

    Optional<CouponHistoryEntity> findByUserIdAndCouponId(Long userId, Long couponId);
}
