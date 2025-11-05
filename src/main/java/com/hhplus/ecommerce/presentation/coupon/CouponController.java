package com.hhplus.ecommerce.presentation.coupon;

import com.hhplus.ecommerce.presentation.coupon.req.ValidateCouponRequest;
import com.hhplus.ecommerce.presentation.coupon.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Tag(name = "쿠폰", description = "쿠폰 관리 API")
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private static final AtomicLong COUPON_HISTORY_ID_GENERATOR = new AtomicLong(2);
    private static final Map<Long, CouponResponse> MY_COUPONS = new LinkedHashMap<>();
    private static final Map<Long, Integer> COUPON_STOCK = new HashMap<>();

    static {
        // 쿠폰 재고 초기화
        COUPON_STOCK.put(1L, 100);
        COUPON_STOCK.put(2L, 50);

        // 초기 발급 쿠폰
        MY_COUPONS.put(1L, new CouponResponse(
            1L, 1L, "신규 회원 10% 할인 쿠폰", "PERCENT", 10, 10000, 5000,
            "2024-10-30T00:00:00", "2024-11-30T23:59:59", "ISSUED",
            "2024-10-30T00:00:00", null
        ));
    }

    // 쿠폰 발급 (POST /api/coupons/{couponId}/issue)
    @Operation(summary = "쿠폰 발급", description = "선착순 쿠폰을 발급받습니다. 한정 수량이며, 동시성 제어가 적용됩니다.")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<IssueCouponResponse> issueCoupon(@PathVariable Long couponId) {
        // 재고 확인
        Integer stock = COUPON_STOCK.getOrDefault(couponId, 0);
        if (stock <= 0) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }

        // 이미 발급 확인 (간단한 Mock)
        boolean alreadyIssued = MY_COUPONS.values().stream()
                .anyMatch(c -> c.getCouponId().equals(couponId) && "ISSUED".equals(c.getStatus()));

        if (alreadyIssued) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        // 재고 차감
        COUPON_STOCK.put(couponId, stock - 1);

        Long historyId = COUPON_HISTORY_ID_GENERATOR.getAndIncrement();
        IssueCouponResponse response = new IssueCouponResponse(
            historyId,
            couponId,
            "신규 회원 10% 할인 쿠폰",
            "PERCENT",
            10,
            LocalDateTime.now().toString(),
            LocalDateTime.now().plusDays(30).toString(),
            "ISSUED",
            LocalDateTime.now().toString()
        );

        MY_COUPONS.put(historyId, new CouponResponse(
            historyId, couponId, "신규 회원 10% 할인 쿠폰", "PERCENT", 10, 10000, 5000,
            response.getValidFrom(), response.getValidUntil(), "ISSUED", response.getIssuedAt(), null
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 내 쿠폰 조회 (GET /api/coupons/me)
    @Operation(summary = "내 쿠폰 조회", description = "발급받은 쿠폰 목록을 조회합니다.")
    @GetMapping("/me")
    public MyCouponListResponse getMyCoupons(@RequestParam(required = false) String status) {
        List<CouponResponse> coupons = new ArrayList<>(MY_COUPONS.values());

        if (status != null) {
            coupons = coupons.stream()
                    .filter(c -> status.equals(c.getStatus()))
                    .collect(Collectors.toList());
        }

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

        if (request.getOrderAmount() < coupon.getUseMinAmount()) {
            return new ValidateCouponResponse(
                false, null, null,
                String.format("최소 주문 금액(%,d원)을 충족하지 못했습니다.", coupon.getUseMinAmount())
            );
        }

        int discountAmount;
        if ("PERCENT".equals(coupon.getDiscountType())) {
            discountAmount = request.getOrderAmount() * coupon.getDiscountAmount() / 100;
            discountAmount = Math.min(discountAmount, coupon.getUseMaxAmount());
        } else {
            discountAmount = coupon.getDiscountAmount();
        }

        int finalAmount = request.getOrderAmount() - discountAmount;

        return new ValidateCouponResponse(
            true, discountAmount, finalAmount, "쿠폰을 사용할 수 있습니다."
        );
    }
}
