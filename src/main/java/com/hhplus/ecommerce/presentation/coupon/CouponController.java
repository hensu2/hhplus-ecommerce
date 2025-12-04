package com.hhplus.ecommerce.presentation.coupon;

import com.hhplus.ecommerce.application.coupon.CreateCouponUseCase;
import com.hhplus.ecommerce.application.coupon.GetCouponUseCase;
import com.hhplus.ecommerce.application.coupon.GetCouponsUseCase;
import com.hhplus.ecommerce.application.coupon.GetMyCouponsUseCase;
import com.hhplus.ecommerce.application.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.presentation.coupon.req.CreateCouponRequest;
import com.hhplus.ecommerce.presentation.coupon.req.ValidateCouponRequest;
import com.hhplus.ecommerce.presentation.coupon.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@Tag(name = "쿠폰", description = "쿠폰 관리 API")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;
    private final GetCouponsUseCase getCouponsUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final GetMyCouponsUseCase getMyCouponsUseCase;
    private final GetCouponUseCase getCouponUseCase;

    private static final Map<Long, CouponResponse> MY_COUPONS = new LinkedHashMap<>();

    static {
        // 초기 발급 쿠폰 (테스트용)
        MY_COUPONS.put(1L, new CouponResponse(
            1L, 1L, "신규 회원 10% 할인 쿠폰", "PERCENT", 10, 10000, 5000,
            "2024-10-30T00:00:00", "2024-11-30T23:59:59", "ISSUED",
            "2024-10-30T00:00:00", null
        ));
    }

    // 쿠폰 생성 (POST /api/coupons)
    @Operation(summary = "쿠폰 생성", description = "관리자가 선착순 쿠폰을 생성합니다.")
    @PostMapping
    public ResponseEntity<CreateCouponResponse> createCoupon(@RequestBody CreateCouponRequest request) {
        CouponEntity coupon = createCouponUseCase.execute(request);
        CreateCouponResponse response = CreateCouponResponse.from(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 쿠폰 목록 조회 (GET /api/coupons)
    @Operation(summary = "쿠폰 목록 조회", description = "발급 가능한 쿠폰 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CouponListResponse>> getCoupons() {
        List<CouponEntity> coupons = getCouponsUseCase.execute();
        List<CouponListResponse> response = coupons.stream()
                .map(CouponEntity::toCouponListResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // 쿠폰 발급 (POST /api/coupons/{couponId}/issue)
    @Operation(summary = "쿠폰 발급", description = "선착순 쿠폰을 발급받습니다. 한정 수량이며, 동시성 제어가 적용됩니다.")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<IssueCouponResponse> issueCoupon(
            @PathVariable Long couponId,
            @RequestParam Long userId) {
        var history = issueCouponUseCase.execute(userId, couponId);
        var coupon = getCouponUseCase.execute(couponId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new IssueCouponResponse(history, coupon));
    }

    @Operation(summary = "내 쿠폰 조회", description = "발급받은 쿠폰 목록을 조회합니다.")
    @GetMapping("/me")
    public MyCouponListResponse getMyCoupons(
            @RequestParam Long userId,
            @RequestParam(required = false) String status) {
        CouponStatus couponStatus = status != null ? CouponStatus.valueOf(status) : null;
        var histories = getMyCouponsUseCase.execute(userId, couponStatus);

        List<CouponResponse> coupons = histories.stream()
                .map(history -> {
                    var coupon = getCouponUseCase.execute(history.getCouponId());
                    return new CouponResponse(history, coupon);
                })
                .toList();
        return new MyCouponListResponse(coupons);
    }

    // 쿠폰 유효성 검증 (POST /api/coupons/{couponHistoryId}/validate)
    @Operation(summary = "쿠폰 유효성 검증", description = "주문 금액에 대해 쿠폰 사용 가능 여부를 확인합니다.")
    @PostMapping("/{couponHistoryId}/validate")
    public ValidateCouponResponse validateCoupon(
            @PathVariable Long couponHistoryId,
            @RequestBody ValidateCouponRequest request) {

        CouponResponse coupon = MY_COUPONS.get(couponHistoryId);
        if (coupon == null) {
            return new ValidateCouponResponse(
                false, null, null, "쿠폰을 찾을 수 없습니다."
            );
        }

        if (request.orderAmount() < coupon.useMinAmount()) {
            return new ValidateCouponResponse(
                false, null, null,
                String.format("최소 주문 금액(%,d원)을 충족하지 못했습니다.", coupon.useMinAmount())
            );
        }

        int discountAmount;
        if ("PERCENT".equals(coupon.discountType())) {
            discountAmount = request.orderAmount() * coupon.discountAmount() / 100;
            discountAmount = Math.min(discountAmount, coupon.useMaxAmount());
        } else {
            discountAmount = coupon.discountAmount();
        }

        int finalAmount = request.orderAmount() - discountAmount;

        return new ValidateCouponResponse(
            true, discountAmount, finalAmount, "쿠폰을 사용할 수 있습니다."
        );
    }
}
