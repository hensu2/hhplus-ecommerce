package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    public List<PaymentEntity> execute(long userId) {
        return paymentRepository.findByUserId(userId);
    }
}
