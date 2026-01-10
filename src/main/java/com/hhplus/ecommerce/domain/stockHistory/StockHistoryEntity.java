package com.hhplus.ecommerce.domain.stockHistory;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 재고 변경 이력 엔티티
 * Kafka 이벤트로 수신한 재고 변경 내역을 저장
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "stock_history")
public class StockHistoryEntity {

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

    @Column(name = "previous_stock", nullable = false)
    private Long previousStock;

    @Column(name = "current_stock", nullable = false)
    private Long currentStock;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;

    @Column(name = "change_reason", nullable = false)
    private String changeReason; // ORDER_CREATED, ORDER_CANCELLED, ADMIN_UPDATE, DIRECT_DECREASE

    @Column(name = "event_type", nullable = false)
    private String eventType; // STOCK_INCREASED, STOCK_DECREASED, STOCK_UPDATED

    @Column(nullable = false)
    private Long timestamp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    public static StockHistoryEntity from(
        Long productOptionId,
        Long productId,
        String productName,
        String optionType,
        Long previousStock,
        Long currentStock,
        Integer changeAmount,
        String changeReason,
        String eventType,
        Long timestamp
    ) {
        return new StockHistoryEntity(
            null,
            productOptionId,
            productId,
            productName,
            optionType,
            previousStock,
            currentStock,
            changeAmount,
            changeReason,
            eventType,
            timestamp,
            null
        );
    }
}
