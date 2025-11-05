package com.hhplus.ecommerce.presentation.payment;

import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import com.hhplus.ecommerce.presentation.payment.res.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Tag(name = "결제", description = "결제 관리 API")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final AtomicLong PAYMENT_ID_GENERATOR = new AtomicLong(2);
    private static final Map<Long, PaymentResponse> PAYMENTS = new HashMap<>();
    private static int userPoint = 100000; // Mock 사용자 포인트

    static {
        // 초기 데이터
        PAYMENTS.put(1L, new PaymentResponse(
            1L, 1L, 1L, 40000, "POINT", "SUCCESS", "2024-10-30T00:00:00", 400
        ));
    }

    // 결제 처리 (POST /api/payments)
    @Operation(summary = "결제 처리", description = "주문에 대한 결제를 처리합니다. 포인트로 결제되며, 성공 시 외부 시스템에 주문 데이터를 비동기로 전송합니다.")
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody ProcessPaymentRequest request) {
        // Mock 주문 금액
        int paymentAmount = 40000;

        // 포인트 잔액 확인
        if (userPoint < paymentAmount) {
            throw new RuntimeException("포인트 잔액이 부족합니다.");
        }

        // 이미 결제된 주문 확인
        boolean alreadyPaid = PAYMENTS.values().stream()
                .anyMatch(p -> p.getOrderId().equals(request.getOrderId()) && "SUCCESS".equals(p.getStatus()));

        if (alreadyPaid) {
            throw new RuntimeException("이미 결제된 주문입니다.");
        }

        // 포인트 차감
        userPoint -= paymentAmount;

        // 적립 포인트 계산 (결제 금액의 1%)
        int earnedPoint = (int) (paymentAmount * 0.01);
        userPoint += earnedPoint;

        Long paymentId = PAYMENT_ID_GENERATOR.getAndIncrement();
        PaymentResponse payment = new PaymentResponse(
            paymentId,
            request.getOrderId(),
            1L,
            paymentAmount,
            request.getPaymentMethod(),
            "SUCCESS",
            LocalDateTime.now().toString(),
            earnedPoint
        );

        PAYMENTS.put(request.getOrderId(), payment);
        return ResponseEntity.ok(payment);
    }

    // 결제 조회 (GET /api/payments/{orderId})
    @Operation(summary = "결제 조회", description = "특정 주문의 결제 정보를 조회합니다.")
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long orderId) {
        PaymentResponse payment = PAYMENTS.get(orderId);

        if (payment == null) {
            throw new RuntimeException("결제 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(payment);
    }
}
