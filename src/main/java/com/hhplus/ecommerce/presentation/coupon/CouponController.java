package com.hhplus.ecommerce.presentation.coupon;

import com.hhplus.ecommerce.application.coupon.GetCouponsUseCase;
import com.hhplus.ecommerce.application.coupon.GetMyCouponsUseCase;
import com.hhplus.ecommerce.application.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.application.coupon.ValidateCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.presentation.coupon.req.ValidateCouponRequest;
import com.hhplus.ecommerce.presentation.coupon.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "쿠폰", description = "쿠폰 관리 API")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final GetCouponsUseCase getCouponsUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final GetMyCouponsUseCase getMyCouponsUseCase;
    private final ValidateCouponUseCase validateCouponUseCase;

    // 쿠폰 목록 조회 (GET /api/coupons)
    @Operation(summary = "쿠폰 목록 조회", description = "발급 가능한 쿠폰 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CouponListResponse>> getCoupons() {
        List<CouponListResponse> response = getCouponsUseCase.execute();
        return ResponseEntity.ok(response);
    }

    // 쿠폰 발급 (POST /api/coupons/{couponId}/issue)
    @Operation(summary = "쿠폰 발급", description = "선착순 쿠폰을 발급받습니다. 한정 수량이며, 동시성 제어가 적용됩니다.")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<IssueCouponResponse> issueCoupon(
            @PathVariable Long couponId,
            @RequestParam Long userId) {
        IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 내 쿠폰 조회 (GET /api/coupons/me)
    @Operation(summary = "내 쿠폰 조회", description = "발급받은 쿠폰 목록을 조회합니다.")
    @GetMapping("/me")
    public MyCouponListResponse getMyCoupons(
            @RequestParam Long userId,
            @RequestParam(required = false) String status) {
        CouponStatus couponStatus = status != null ? CouponStatus.valueOf(status) : null;
        List<CouponResponse> coupons = getMyCouponsUseCase.execute(userId, couponStatus);
        return new MyCouponListResponse(coupons);
    }

    // 쿠폰 유효성 검증 (POST /api/coupons/{couponHistoryId}/validate)
    @Operation(summary = "쿠폰 유효성 검증", description = "주문 금액에 대해 쿠폰 사용 가능 여부를 확인합니다.")
    @PostMapping("/{couponHistoryId}/validate")
    public ValidateCouponResponse validateCoupon(
            @PathVariable Long couponHistoryId,
            @RequestBody ValidateCouponRequest request) {
        return validateCouponUseCase.execute(couponHistoryId, request.orderAmount());
    }
}
