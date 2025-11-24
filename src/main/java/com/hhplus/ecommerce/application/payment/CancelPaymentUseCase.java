package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentEntity execute(long paymentId) {
        PaymentEntity payment = paymentRepository.getOrThrow(paymentId);

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 결제입니다.");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 결제만 취소할 수 있습니다.");
        }

        long now = System.currentTimeMillis();
        PaymentEntity cancelledPayment = new PaymentEntity(
            payment.getId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getAmount(),
            PaymentStatus.CANCELLED,
            payment.getCreatedAt(),
            now
        );
        return paymentRepository.save(cancelledPayment);
    }
}
