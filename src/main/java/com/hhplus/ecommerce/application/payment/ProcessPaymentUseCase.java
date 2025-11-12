package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import com.hhplus.ecommerce.presentation.payment.res.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public PaymentResponse execute(ProcessPaymentRequest request) {
        long now = System.currentTimeMillis();

        // 1. 결제 금액 검증
        if (request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("결제 금액이 유효하지 않습니다.");
        }

        // 2. 결제 처리 (외부 PG사 연동은 생략, 성공으로 가정)
        PaymentStatus paymentStatus = PaymentStatus.COMPLETED;

        // 3. 결제 정보 저장
        PaymentEntity payment = new PaymentEntity(
            0L,
            request.orderId(),
            request.userId(),
            request.amount(),
            paymentStatus,
            now,
            now
        );
        PaymentEntity savedPayment = paymentRepository.save(payment);

        // 4. 응답 생성
        return new PaymentResponse(
            savedPayment.id(),
            savedPayment.orderId(),
            savedPayment.userId(),
            savedPayment.amount(),
            savedPayment.status().name(),
            formatTimestamp(savedPayment.createdAt())
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
