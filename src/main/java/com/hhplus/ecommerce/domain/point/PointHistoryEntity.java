package com.hhplus.ecommerce.domain.point;

import com.hhplus.ecommerce.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_HISTORY", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_point_history_user"))
    private UserEntity user;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private PointHistoryEntity(UserEntity user, Long amount, TransactionType transactionType, String description) {
        this.user = user;
        this.amount = amount;
        this.transactionType = transactionType;
        this.description = description;
    }

    /**
     * 포인트 적립 내역 생성
     */
    public static PointHistoryEntity createEarn(UserEntity user, Long amount, String description) {
        validateAmount(amount);
        validateUser(user);
        return new PointHistoryEntity(user, amount, TransactionType.EARN, description);
    }

    /**
     * 포인트 사용 내역 생성
     */
    public static PointHistoryEntity createUse(UserEntity user, Long amount, String description) {
        validateAmount(amount);
        validateUser(user);
        return new PointHistoryEntity(user, amount, TransactionType.USE, description);
    }

    /**
     * 포인트 환불 내역 생성
     */
    public static PointHistoryEntity createRefund(UserEntity user, Long amount, String description) {
        validateAmount(amount);
        validateUser(user);
        return new PointHistoryEntity(user, amount, TransactionType.REFUND, description);
    }

    private static void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("포인트 금액은 0보다 커야 합니다.");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자 정보는 필수입니다.");
        }
    }
}