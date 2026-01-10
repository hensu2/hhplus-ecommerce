package com.hhplus.ecommerce.infrastructure.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.infrastructure.coupon.jpa.CouponHistoryJpaRepository;
import com.hhplus.ecommerce.infrastructure.coupon.jpa.CouponJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;
    private final CouponHistoryJpaRepository couponHistoryJpaRepository;

    @Override
    public List<CouponEntity> findAll() {
        return couponJpaRepository.findAll();
    }

    @Override
    public Optional<CouponEntity> findById(long couponId) {
        return couponJpaRepository.findById(couponId);
    }

    @Override
    public CouponEntity save(CouponEntity coupon) {
        return couponJpaRepository.save(coupon);
    }

    @Override
    public CouponHistoryEntity saveHistory(CouponHistoryEntity history) {
        return couponHistoryJpaRepository.save(history);
    }

    @Override
    public Optional<CouponHistoryEntity> findHistoryByUserIdAndCouponId(long userId, long couponId) {
        return couponHistoryJpaRepository.findByUserIdAndCouponId(userId, couponId);
    }

    @Override
    public List<CouponHistoryEntity> findHistoriesByUserId(long userId) {
        return couponHistoryJpaRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public CouponEntity decreaseStock(long couponId) {
        // 동시성 제어는 IssueCouponUseCase에서 Redisson 분산 락으로 처리
        CouponEntity coupon = couponJpaRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        coupon.decreaseStock();
        return couponJpaRepository.save(coupon);
    }

    @Override
    public List<CouponHistoryEntity> saveAllHistories(List<CouponHistoryEntity> histories) {
        return couponHistoryJpaRepository.saveAll(histories);
    }
}
