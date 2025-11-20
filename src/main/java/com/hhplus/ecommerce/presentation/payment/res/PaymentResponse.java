package com.hhplus.ecommerce.presentation.payment.res;

import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.payment.PaymentEntity;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        Long userId,
        Integer amount,
        String status,
        String createdAt
) {
    public PaymentResponse(PaymentEntity payment) {
        this(
            payment.getId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getStatus().name(),
            DateTimeUtils.toLocalDateTime(payment.getCreatedAt())
        );
    }
}
