package com.example.demo.controller.payment;

import com.example.demo.dto.request.PaymentRequest;
import com.example.demo.dto.response.PaymentResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Operation(
            summary = "결제 처리",
            description = "주문에 대한 결제를 처리합니다. 포인트로 결제되며, 성공 시 외부 시스템에 주문 데이터를 비동기로 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "포인트 부족 또는 이미 결제됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "결제 요청",
                    required = true
            )
            @RequestBody PaymentRequest request
    ) {
        // Mock 데이터 생성
        PaymentResponse response = new PaymentResponse(
                1L,
                request.getOrderId(),
                1L,
                40000,
                request.getPaymentMethod(),
                "SUCCESS",
                LocalDateTime.now(),
                400  // 결제 금액의 1% 적립
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "결제 조회",
            description = "특정 주문의 결제 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 조회 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "결제 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long orderId
    ) {
        // Mock 데이터 생성
        PaymentResponse response = new PaymentResponse(
                1L,
                orderId,
                1L,
                40000,
                "POINT",
                "SUCCESS",
                LocalDateTime.now(),
                null
        );

        return ResponseEntity.ok(response);
    }

    @Schema(description = "에러 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorResponse {
        @Schema(description = "에러 코드", example = "INSUFFICIENT_BALANCE")
        private String error;

        @Schema(description = "에러 메시지", example = "포인트 잔액이 부족합니다.")
        private String message;

        @Schema(description = "현재 잔액", example = "30000")
        private Integer currentBalance;

        @Schema(description = "필요 금액", example = "40000")
        private Integer requiredAmount;
    }
}