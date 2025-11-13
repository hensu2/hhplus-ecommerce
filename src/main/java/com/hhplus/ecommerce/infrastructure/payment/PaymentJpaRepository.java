package com.hhplus.ecommerce.infrastructure.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByUserId(Long userId);
}
