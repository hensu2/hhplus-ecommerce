package com.hhplus.ecommerce.domain.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount;

    @Column(name = "coupon_history_id")
    private Long couponHistoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    private OrderEntity(Long id, Long userId, Integer totalAmount, Integer discountAmount, Integer finalAmount,
                        Long couponHistoryId, OrderStatus status, Long createdAt, Long updatedAt) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.couponHistoryId = couponHistoryId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderEntity create(Long userId, Integer totalAmount, Integer discountAmount, Integer finalAmount, Long couponHistoryId) {
        long now = System.currentTimeMillis();
        return new OrderEntity(null, userId, totalAmount, discountAmount, finalAmount, couponHistoryId, OrderStatus.PENDING, now, now);
    }

    public static OrderEntity createForTest(Long id, Long userId, Integer totalAmount, Integer discountAmount,
                                            Integer finalAmount, Long couponHistoryId, OrderStatus status, Long createdAt, Long updatedAt) {
        return new OrderEntity(id, userId, totalAmount, discountAmount, finalAmount, couponHistoryId, status, createdAt, updatedAt);
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = System.currentTimeMillis();
    }
}
