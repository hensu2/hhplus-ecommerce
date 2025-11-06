package com.hhplus.ecommerce.infrastructure.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.infrastructure.coupon.memory.CouponHistoryTable;
import com.hhplus.ecommerce.infrastructure.coupon.memory.CouponTable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponTable couponTable;
    private final CouponHistoryTable couponHistoryTable;

    public CouponRepositoryImpl(CouponTable couponTable, CouponHistoryTable couponHistoryTable) {
        this.couponTable = couponTable;
        this.couponHistoryTable = couponHistoryTable;
    }

    @Override
    public List<CouponEntity> findAll() {
        return couponTable.findAll();
    }

    @Override
    public Optional<CouponEntity> findById(long couponId) {
        return couponTable.findById(couponId);
    }

    @Override
    public CouponEntity save(CouponEntity coupon) {
        return couponTable.save(coupon);
    }

    @Override
    public CouponHistoryEntity saveHistory(CouponHistoryEntity history) {
        return couponHistoryTable.save(history);
    }

    @Override
    public Optional<CouponHistoryEntity> findHistoryByUserIdAndCouponId(long userId, long couponId) {
        return couponHistoryTable.findByUserIdAndCouponId(userId, couponId);
    }

    @Override
    public List<CouponHistoryEntity> findHistoriesByUserId(long userId) {
        return couponHistoryTable.findByUserId(userId);
    }
}
