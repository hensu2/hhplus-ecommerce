package com.hhplus.ecommerce.infrastructure.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.infrastructure.coupon.memory.CouponTable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponTable couponTable;

    public CouponRepositoryImpl(CouponTable couponTable) {
        this.couponTable = couponTable;
    }

    @Override
    public List<CouponEntity> findAll() {
        return couponTable.findAll();
    }
}
