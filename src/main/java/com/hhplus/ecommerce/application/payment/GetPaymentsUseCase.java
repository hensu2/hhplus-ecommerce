package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.res.PaymentResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentsUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentResponse> execute(long userId) {
        List<PaymentEntity> payments = paymentRepository.findByUserId(userId);

        return payments.stream()
            .map(this::toPaymentResponse)
            .collect(Collectors.toList());
    }

    private PaymentResponse toPaymentResponse(PaymentEntity payment) {
        return new PaymentResponse(
            payment.id(),
            payment.orderId(),
            payment.userId(),
            payment.amount(),
            payment.status().name(),
            formatTimestamp(payment.createdAt())
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
