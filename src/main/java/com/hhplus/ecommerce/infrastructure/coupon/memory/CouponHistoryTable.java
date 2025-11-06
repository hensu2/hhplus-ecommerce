package com.hhplus.ecommerce.infrastructure.coupon.memory;

import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class CouponHistoryTable {
    private final ConcurrentHashMap<Long, CouponHistoryEntity> table = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public CouponHistoryEntity save(CouponHistoryEntity history) {
        long id = history.id() == 0 ? idGenerator.getAndIncrement() : history.id();
        CouponHistoryEntity newHistory = new CouponHistoryEntity(
            id,
            history.userId(),
            history.couponId(),
            history.status(),
            history.issuedAt(),
            history.usedAt()
        );
        table.put(id, newHistory);
        return newHistory;
    }

    public Optional<CouponHistoryEntity> findByUserIdAndCouponId(long userId, long couponId) {
        return table.values().stream()
            .filter(h -> h.userId() == userId && h.couponId() == couponId)
            .findFirst();
    }

    public List<CouponHistoryEntity> findByUserId(long userId) {
        return table.values().stream()
            .filter(h -> h.userId() == userId)
            .collect(Collectors.toList());
    }
}
