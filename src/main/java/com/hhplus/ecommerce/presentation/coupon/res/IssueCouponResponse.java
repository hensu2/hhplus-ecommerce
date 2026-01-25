package com.hhplus.ecommerce.presentation.coupon.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IssueCouponResponse {
    private String status;
    private String message;
    private Long couponId;
    private Long userId;
    private Long requestedAt;

    // 기존 호환성을 위한 정적 팩토리 메서드 (DB 동기화 완료 후)
    public static IssueCouponCompletedResponse fromCompleted(CouponHistoryEntity history, CouponEntity coupon) {
        return new IssueCouponCompletedResponse(
            history.getId(),
            history.getCouponId(),
            coupon.getCouponName(),
            coupon.getDiscountType().name(),
            coupon.getDiscountAmount(),
            DateTimeUtils.toLocalDateTime(coupon.getValidFrom()),
            DateTimeUtils.toLocalDateTime(coupon.getValidUntil()),
            history.getStatus().name(),
            DateTimeUtils.toLocalDateTime(history.getIssuedAt())
        );
    }

    // 기존 응답 형식을 위한 내부 클래스
    @Getter
    @AllArgsConstructor
    public static class IssueCouponCompletedResponse {
        private Long id;
        private Long couponId;
        private String couponName;
        private String discountType;
        private Integer discountAmount;
        private String validFrom;
        private String validUntil;
        private String status;
        private String issuedAt;
    }
}
