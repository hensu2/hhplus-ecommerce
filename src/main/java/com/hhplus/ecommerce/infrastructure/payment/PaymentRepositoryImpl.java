package com.hhplus.ecommerce.infrastructure.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public PaymentEntity save(PaymentEntity payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<PaymentEntity> findById(long paymentId) {
        return paymentJpaRepository.findById(paymentId);
    }

    @Override
    public List<PaymentEntity> findByUserId(long userId) {
        return paymentJpaRepository.findByUserId(userId);
    }
}
