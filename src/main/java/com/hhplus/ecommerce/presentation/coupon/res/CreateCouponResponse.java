package com.hhplus.ecommerce.presentation.coupon.res;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 생성 응답")
public record CreateCouponResponse(
    @Schema(description = "쿠폰 ID")
    Long id,

    @Schema(description = "쿠폰명")
    String couponName,

    @Schema(description = "할인 타입")
    String discountType,

    @Schema(description = "할인 금액/비율")
    Integer discountAmount,

    @Schema(description = "최소 사용 금액")
    Integer useMinAmount,

    @Schema(description = "최대 할인 금액")
    Integer useMaxAmount,

    @Schema(description = "발급 가능 수량")
    Integer stock,

    @Schema(description = "유효 시작 일시")
    Long validFrom,

    @Schema(description = "유효 종료 일시")
    Long validUntil,

    @Schema(description = "생성 일시")
    Long createdAt
) {
    public static CreateCouponResponse from(CouponEntity coupon) {
        return new CreateCouponResponse(
            coupon.getId(),
            coupon.getCouponName(),
            coupon.getDiscountType().name(),
            coupon.getDiscountAmount(),
            coupon.getUseMinAmount(),
            coupon.getUseMaxAmount(),
            coupon.getStock(),
            coupon.getValidFrom(),
            coupon.getValidUntil(),
            coupon.getCreatedAt()
        );
    }
}