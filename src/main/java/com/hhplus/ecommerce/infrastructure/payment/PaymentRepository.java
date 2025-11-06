package com.hhplus.ecommerce.infrastructure.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    PaymentEntity save(PaymentEntity payment);
    Optional<PaymentEntity> findById(long paymentId);
    List<PaymentEntity> findByUserId(long userId);
}
