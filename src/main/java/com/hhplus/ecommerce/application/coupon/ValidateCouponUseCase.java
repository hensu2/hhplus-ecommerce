package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.CouponRepository;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.presentation.coupon.res.ValidateCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateCouponUseCase {

    private final CouponRepository couponRepository;

    public ValidateCouponResponse execute(long couponHistoryId, int orderAmount) {
        // 1. 쿠폰 히스토리 조회
        CouponHistoryEntity history = couponRepository.findHistoryById(couponHistoryId)
            .orElse(null);

        if (history == null) {
            return new ValidateCouponResponse(
                false, null, null, "쿠폰을 찾을 수 없습니다."
            );
        }

        // 2. 쿠폰이 이미 사용되었는지 확인
        if (history.getStatus() == CouponStatus.USED) {
            return new ValidateCouponResponse(
                false, null, null, "이미 사용된 쿠폰입니다."
            );
        }

        // 3. 쿠폰 정보 조회
        CouponEntity coupon = couponRepository.findById(history.getCouponId())
            .orElseThrow(() -> new IllegalStateException("쿠폰 정보를 찾을 수 없습니다."));

        // 4. 최소 주문 금액 확인
        if (orderAmount < coupon.getUseMinAmount()) {
            return new ValidateCouponResponse(
                false, null, null,
                "최소 주문 금액(" + coupon.getUseMinAmount() + "원)을 충족하지 못했습니다."
            );
        }

        // 5. 할인 금액 계산
        int discountAmount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discountAmount = orderAmount * coupon.getDiscountAmount() / 100;
            discountAmount = Math.min(discountAmount, coupon.getUseMaxAmount());
        } else {
            discountAmount = coupon.getDiscountAmount();
        }

        int finalAmount = orderAmount - discountAmount;

        return new ValidateCouponResponse(
            true, discountAmount, finalAmount, "쿠폰을 사용할 수 있습니다."
        );
    }
}
