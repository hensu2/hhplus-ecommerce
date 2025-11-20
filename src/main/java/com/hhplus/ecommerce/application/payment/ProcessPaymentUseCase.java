package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public PaymentEntity execute(ProcessPaymentRequest request) {
        long now = System.currentTimeMillis();

        if (request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("결제 금액이 유효하지 않습니다.");
        }

        PaymentStatus paymentStatus = PaymentStatus.COMPLETED;

        PaymentEntity payment = new PaymentEntity(
            0L,
            request.orderId(),
            request.userId(),
            request.amount(),
            paymentStatus,
            now,
            now
        );
        return paymentRepository.save(payment);
    }
}
