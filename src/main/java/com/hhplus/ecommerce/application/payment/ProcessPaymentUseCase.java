package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentEntity execute(ProcessPaymentRequest request) {
        long now = System.currentTimeMillis();

        if (request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("결제 금액이 유효하지 않습니다.");
        }

        // UNIQUE 제약조건 체크 - 같은 주문에 대한 결제가 이미 존재하는지 확인
        java.util.List<PaymentEntity> existingPayments = paymentRepository.findByOrderId(request.orderId());
        if (!existingPayments.isEmpty()) {
            throw new IllegalStateException("이미 처리된 주문입니다. Order ID: " + request.orderId());
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
