package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;

    public CouponHistoryEntity execute(long userId, long couponId) {
        CouponEntity coupon = couponRepository.getOrThrow(couponId);

        couponRepository.findHistoryByUserIdAndCouponId(userId, couponId)
            .ifPresent(history -> {
                throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
            });

        couponRepository.decreaseStock(couponId);

        long now = System.currentTimeMillis();
        CouponHistoryEntity history = new CouponHistoryEntity(
            0L,
            userId,
            couponId,
            CouponStatus.ISSUED,
            now,
            null
        );
        return couponRepository.saveHistory(history);
    }
}
