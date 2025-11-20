package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetMyCouponsUseCase {

    private final CouponRepository couponRepository;

    public List<CouponHistoryEntity> execute(long userId, CouponStatus status) {
        List<CouponHistoryEntity> histories = couponRepository.findHistoriesByUserId(userId);

        return histories.stream()
            .filter(history -> status == null || history.getStatus() == status)
            .toList();
    }
}
