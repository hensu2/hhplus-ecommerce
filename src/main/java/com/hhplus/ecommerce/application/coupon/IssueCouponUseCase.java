package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;

    public IssueCouponResponse execute(long userId, long couponId) {
        // 1. 쿠폰 존재 여부 확인
        CouponEntity coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 2. 이미 발급받았는지 확인
        couponRepository.findHistoryByUserIdAndCouponId(userId, couponId)
            .ifPresent(history -> {
                throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
            });

        // 3. 재고 차감 (원자적 연산으로 동시성 제어)
        CouponEntity updatedCoupon = couponRepository.decreaseStock(couponId);

        // 4. 발급 히스토리 저장
        long now = System.currentTimeMillis();
        CouponHistoryEntity history = new CouponHistoryEntity(
            0L,
            userId,
            couponId,
            CouponStatus.ISSUED,
            now,
            null
        );
        CouponHistoryEntity savedHistory = couponRepository.saveHistory(history);

        // 5. 응답 생성
        return savedHistory.toIssueCouponResponse(updatedCoupon);
    }
}
