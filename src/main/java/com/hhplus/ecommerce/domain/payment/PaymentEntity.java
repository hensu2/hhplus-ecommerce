package com.hhplus.ecommerce.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    private PaymentEntity(Long id, Long orderId, Long userId, Integer amount, PaymentStatus status, Long createdAt, Long updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentEntity create(Long orderId, Long userId, Integer amount) {
        long now = System.currentTimeMillis();
        return new PaymentEntity(null, orderId, userId, amount, PaymentStatus.PENDING, now, now);
    }

    public static PaymentEntity createForTest(Long id, Long orderId, Long userId, Integer amount, PaymentStatus status, Long createdAt, Long updatedAt) {
        return new PaymentEntity(id, orderId, userId, amount, status, createdAt, updatedAt);
    }

    public void complete() {
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = System.currentTimeMillis();
    }
}
