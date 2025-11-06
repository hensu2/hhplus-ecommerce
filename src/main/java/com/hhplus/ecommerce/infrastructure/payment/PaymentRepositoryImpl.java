package com.hhplus.ecommerce.infrastructure.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.infrastructure.payment.memory.PaymentTable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentTable paymentTable;

    public PaymentRepositoryImpl(PaymentTable paymentTable) {
        this.paymentTable = paymentTable;
    }

    @Override
    public PaymentEntity save(PaymentEntity payment) {
        return paymentTable.save(payment);
    }

    @Override
    public Optional<PaymentEntity> findById(long paymentId) {
        return paymentTable.findById(paymentId);
    }

    @Override
    public List<PaymentEntity> findByUserId(long userId) {
        return paymentTable.findByUserId(userId);
    }
}
