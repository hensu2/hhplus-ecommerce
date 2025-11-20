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
        long id = (history.getId() == null || history.getId() == 0L) ? idGenerator.getAndIncrement() : history.getId();
        CouponHistoryEntity newHistory = new CouponHistoryEntity(
            id,
            history.getUserId(),
            history.getCouponId(),
            history.getStatus(),
            history.getIssuedAt(),
            history.getUsedAt()
        );
        table.put(id, newHistory);
        return newHistory;
    }

    public Optional<CouponHistoryEntity> findByUserIdAndCouponId(long userId, long couponId) {
        return table.values().stream()
            .filter(h -> h.getUserId() == userId && h.getCouponId() == couponId)
            .findFirst();
    }

    public List<CouponHistoryEntity> findByUserId(long userId) {
        return table.values().stream()
            .filter(h -> h.getUserId() == userId)
            .collect(Collectors.toList());
    }
}
