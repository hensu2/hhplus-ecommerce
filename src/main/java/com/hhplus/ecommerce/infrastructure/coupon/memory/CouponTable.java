package com.hhplus.ecommerce.infrastructure.coupon.memory;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CouponTable {
    private final ConcurrentHashMap<Long, CouponEntity> table = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        long now = System.currentTimeMillis();
        LocalDateTime validFrom = LocalDateTime.of(2024, 10, 30, 0, 0, 0);
        LocalDateTime validUntil = LocalDateTime.of(2024, 11, 30, 23, 59, 59);

        long validFromTimestamp = validFrom.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long validUntilTimestamp = validUntil.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        table.put(1L, new CouponEntity(
            1L,
            "신규 회원 10% 할인 쿠폰",
            DiscountType.PERCENT,
            10,
            10000,
            5000,
            100,
            validFromTimestamp,
            validUntilTimestamp,
            now,
            now
        ));

        table.put(2L, new CouponEntity(
            2L,
            "5000원 할인 쿠폰",
            DiscountType.AMOUNT,
            5000,
            20000,
            5000,
            50,
            validFromTimestamp,
            validUntilTimestamp,
            now,
            now
        ));
    }

    public List<CouponEntity> findAll() {
        return new ArrayList<>(table.values());
    }

    public Optional<CouponEntity> findById(long couponId) {
        return Optional.ofNullable(table.get(couponId));
    }

    public CouponEntity save(CouponEntity coupon) {
        table.put(coupon.id(), coupon);
        return coupon;
    }
}
