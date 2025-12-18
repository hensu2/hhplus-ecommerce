package com.hhplus.ecommerce.domain.lowStockAlert;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 재고 부족 알림 엔티티
 * 재고가 임계값 이하로 떨어졌을 때 알림 생성
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "low_stock_alert")
public class LowStockAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "option_type", nullable = false)
    private String optionType;

    @Column(name = "current_stock", nullable = false)
    private Long currentStock;

    @Column(name = "threshold", nullable = false)
    private Long threshold;

    @Column(name = "alert_message", nullable = false)
    private String alertMessage;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "resolved_at")
    private Long resolvedAt;

    public static LowStockAlertEntity create(
        Long productOptionId,
        Long productId,
        String productName,
        String optionType,
        Long currentStock,
        Long threshold
    ) {
        String message = String.format(
            "[재고 부족 알림] %s (%s) - 현재 재고: %d, 임계값: %d",
            productName, optionType, currentStock, threshold
        );

        return new LowStockAlertEntity(
            null,
            productOptionId,
            productId,
            productName,
            optionType,
            currentStock,
            threshold,
            message,
            false,
            null,
            null
        );
    }

    public LowStockAlertEntity resolve() {
        return new LowStockAlertEntity(
            this.id,
            this.productOptionId,
            this.productId,
            this.productName,
            this.optionType,
            this.currentStock,
            this.threshold,
            this.alertMessage,
            true,
            this.createdAt,
            System.currentTimeMillis()
        );
    }
}
