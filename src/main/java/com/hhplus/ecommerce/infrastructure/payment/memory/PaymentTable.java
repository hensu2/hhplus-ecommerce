package com.hhplus.ecommerce.infrastructure.payment.memory;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class PaymentTable {
    private final ConcurrentHashMap<Long, PaymentEntity> table = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public PaymentEntity save(PaymentEntity payment) {
        long id = payment.id() == 0 ? idGenerator.getAndIncrement() : payment.id();
        PaymentEntity newPayment = new PaymentEntity(
            id,
            payment.orderId(),
            payment.userId(),
            payment.amount(),
            payment.status(),
            payment.createdAt(),
            payment.updatedAt()
        );
        table.put(id, newPayment);
        return newPayment;
    }

    public Optional<PaymentEntity> findById(long paymentId) {
        return Optional.ofNullable(table.get(paymentId));
    }

    public List<PaymentEntity> findByUserId(long userId) {
        return table.values().stream()
            .filter(payment -> payment.userId() == userId)
            .collect(Collectors.toList());
    }
}
