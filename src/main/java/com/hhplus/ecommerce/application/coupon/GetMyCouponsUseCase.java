package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMyCouponsUseCase {

    private final CouponRepository couponRepository;

    public List<CouponResponse> execute(long userId, CouponStatus status) {
        List<CouponHistoryEntity> histories = couponRepository.findHistoriesByUserId(userId);

        return histories.stream()
            .filter(history -> status == null || history.status() == status)
            .map(history -> {
                CouponEntity coupon = couponRepository.findById(history.couponId())
                    .orElseThrow(() -> new IllegalStateException("쿠폰 정보를 찾을 수 없습니다."));
                return toCouponResponse(history, coupon);
            })
            .collect(Collectors.toList());
    }

    private CouponResponse toCouponResponse(CouponHistoryEntity history, CouponEntity coupon) {
        return new CouponResponse(
            history.id(),
            history.couponId(),
            coupon.couponName(),
            coupon.discountType().name(),
            coupon.discountAmount(),
            coupon.useMinAmount(),
            coupon.useMaxAmount(),
            formatTimestamp(coupon.validFrom()),
            formatTimestamp(coupon.validUntil()),
            history.status().name(),
            formatTimestamp(history.issuedAt()),
            history.usedAt() != null ? formatTimestamp(history.usedAt()) : null
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
