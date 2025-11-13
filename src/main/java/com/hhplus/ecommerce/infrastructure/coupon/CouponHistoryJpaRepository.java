package com.hhplus.ecommerce.infrastructure.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponHistoryJpaRepository extends JpaRepository<CouponHistoryEntity, Long> {
    Optional<CouponHistoryEntity> findByUserIdAndCouponId(Long userId, Long couponId);
    List<CouponHistoryEntity> findByUserId(Long userId);
}
