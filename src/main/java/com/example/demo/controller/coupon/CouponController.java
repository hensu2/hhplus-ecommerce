package com.example.demo.controller.coupon;

import com.example.demo.dto.request.ValidateCouponRequest;
import com.example.demo.dto.response.CouponIssueResponse;
import com.example.demo.dto.response.CouponResponse;
import com.example.demo.dto.response.CouponsResponse;
import com.example.demo.dto.response.ValidateCouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Coupon", description = "쿠폰 API")
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Operation(
            summary = "쿠폰 발급 (선착순)",
            description = "선착순 쿠폰을 발급받습니다. 한정 수량이며, 동시성 제어가 적용됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "쿠폰 발급 성공",
                    content = @Content(schema = @Schema(implementation = CouponIssueResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "쿠폰 소진 또는 이미 발급",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "요청 제한 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "쿠폰 ID", required = true)
            @PathVariable Long couponId
    ) {
        // Mock 데이터 생성
        CouponIssueResponse response = new CouponIssueResponse(
                1L,
                couponId,
                "신규 회원 10% 할인 쿠폰",
                "PERCENT",
                10,
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(1),
                "ISSUED",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "내 쿠폰 조회",
            description = "발급받은 쿠폰 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "쿠폰 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CouponsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<CouponsResponse> getMyCoupons(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "쿠폰 상태 (ISSUED, USED, EXPIRED)")
            @RequestParam(required = false) String status
    ) {
        // Mock 데이터 생성
        List<CouponResponse> coupons = new ArrayList<>();

        CouponResponse coupon1 = new CouponResponse(
                1L, 1L, "신규 회원 10% 할인 쿠폰", "PERCENT", 10,
                10000, 5000,
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1),
                "ISSUED", LocalDateTime.now(), null
        );
        coupons.add(coupon1);

        CouponResponse coupon2 = new CouponResponse(
                2L, 2L, "5,000원 할인 쿠폰", "AMOUNT", 5000,
                30000, 5000,
                LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusDays(1),
                "USED", LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusDays(10)
        );
        coupons.add(coupon2);

        CouponsResponse response = new CouponsResponse(coupons);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "쿠폰 유효성 검증",
            description = "주문 금액에 대해 쿠폰 사용 가능 여부를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검증 성공",
                    content = @Content(schema = @Schema(implementation = ValidateCouponResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "사용 불가",
                    content = @Content(schema = @Schema(implementation = ValidateCouponResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{couponHistoryId}/validate")
    public ResponseEntity<ValidateCouponResponse> validateCoupon(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "쿠폰 발급 내역 ID", required = true)
            @PathVariable Long couponHistoryId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "검증 요청",
                    required = true
            )
            @RequestBody ValidateCouponRequest request
    ) {
        // Mock 데이터 생성
        ValidateCouponResponse response = new ValidateCouponResponse(
                true,
                5000,
                request.getOrderAmount() - 5000,
                "쿠폰을 사용할 수 있습니다."
        );

        return ResponseEntity.ok(response);
    }

    @Schema(description = "에러 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorResponse {
        @Schema(description = "에러 코드", example = "OUT_OF_STOCK")
        private String error;

        @Schema(description = "에러 메시지", example = "쿠폰이 모두 소진되었습니다.")
        private String message;
    }
}