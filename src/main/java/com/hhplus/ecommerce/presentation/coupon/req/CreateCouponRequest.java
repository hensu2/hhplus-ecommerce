package com.hhplus.ecommerce.presentation.coupon.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 생성 요청")
public record CreateCouponRequest(
    @Schema(description = "쿠폰명", example = "신규 회원 10% 할인 쿠폰")
    String couponName,

    @Schema(description = "할인 타입 (PERCENT: 퍼센트, FIXED: 고정 금액)", example = "PERCENT")
    String discountType,

    @Schema(description = "할인 금액/비율 (PERCENT면 10 = 10%, FIXED면 1000 = 1000원)", example = "10")
    Integer discountAmount,

    @Schema(description = "최소 사용 금액", example = "10000")
    Integer useMinAmount,

    @Schema(description = "최대 할인 금액", example = "5000")
    Integer useMaxAmount,

    @Schema(description = "발급 가능 수량 (선착순)", example = "1000")
    Integer stock,

    @Schema(description = "유효 시작 일시 (timestamp)", example = "1732512345678")
    Long validFrom,

    @Schema(description = "유효 종료 일시 (timestamp)", example = "1735190745678")
    Long validUntil
) {
}