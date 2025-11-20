package com.hhplus.ecommerce.infrastructure.payment.jpa;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findByUserId(Long userId);
}
