package com.hhplus.ecommerce.infrastructure.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    PaymentEntity save(PaymentEntity payment);
    Optional<PaymentEntity> findById(long paymentId);
    List<PaymentEntity> findByUserId(long userId);
    List<PaymentEntity> findByOrderId(long orderId);

    default PaymentEntity getOrThrow(long paymentId) {
        return findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));
    }
}
