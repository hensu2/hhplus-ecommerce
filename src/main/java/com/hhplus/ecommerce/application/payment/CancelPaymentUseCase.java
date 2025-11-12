package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.res.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class CancelPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public PaymentResponse execute(long paymentId) {
        // 1. 결제 정보 조회
        PaymentEntity payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // 2. 이미 취소된 결제인지 확인
        if (payment.status() == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 결제입니다.");
        }

        // 3. 완료된 결제만 취소 가능
        if (payment.status() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 결제만 취소할 수 있습니다.");
        }

        // 4. 결제 취소 처리
        long now = System.currentTimeMillis();
        PaymentEntity cancelledPayment = new PaymentEntity(
            payment.id(),
            payment.orderId(),
            payment.userId(),
            payment.amount(),
            PaymentStatus.CANCELLED,
            payment.createdAt(),
            now
        );
        PaymentEntity savedPayment = paymentRepository.save(cancelledPayment);

        // 5. 응답 생성
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
