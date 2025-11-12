package com.hhplus.ecommerce.presentation.payment;

import com.hhplus.ecommerce.application.payment.CancelPaymentUseCase;
import com.hhplus.ecommerce.application.payment.GetPaymentsUseCase;
import com.hhplus.ecommerce.application.payment.ProcessPaymentUseCase;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import com.hhplus.ecommerce.presentation.payment.res.PaymentListResponse;
import com.hhplus.ecommerce.presentation.payment.res.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "결제", description = "결제 관리 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentsUseCase getPaymentsUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

    @Operation(summary = "결제 처리", description = "주문에 대한 결제를 처리합니다.")
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = processPaymentUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "결제 목록 조회", description = "사용자의 결제 내역을 조회합니다.")
    @GetMapping
    public ResponseEntity<PaymentListResponse> getPayments(@RequestParam Long userId) {
        List<PaymentResponse> payments = getPaymentsUseCase.execute(userId);
        return ResponseEntity.ok(new PaymentListResponse(payments));
    }

    @Operation(summary = "결제 취소", description = "완료된 결제를 취소합니다.")
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long paymentId) {
        PaymentResponse response = cancelPaymentUseCase.execute(paymentId);
        return ResponseEntity.ok(response);
    }
}
