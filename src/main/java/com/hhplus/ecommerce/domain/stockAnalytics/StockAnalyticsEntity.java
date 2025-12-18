package com.hhplus.ecommerce.domain.stockAnalytics;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 재고 분석 엔티티
 * 일별 재고 변동 통계를 집계
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "stock_analytics", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_option_id", "date"})
})
public class StockAnalyticsEntity {

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

    @Column(name = "date", nullable = false)
    private String date; // YYYY-MM-DD 형식

    @Column(name = "total_increased", nullable = false)
    private Long totalIncreased;

    @Column(name = "total_decreased", nullable = false)
    private Long totalDecreased;

    @Column(name = "increase_count", nullable = false)
    private Integer increaseCount;

    @Column(name = "decrease_count", nullable = false)
    private Integer decreaseCount;

    @Column(name = "order_created_count", nullable = false)
    private Integer orderCreatedCount;

    @Column(name = "order_cancelled_count", nullable = false)
    private Integer orderCancelledCount;

    @Column(name = "admin_update_count", nullable = false)
    private Integer adminUpdateCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public static StockAnalyticsEntity create(
        Long productOptionId,
        Long productId,
        String productName,
        String optionType,
        String date
    ) {
        return new StockAnalyticsEntity(
            null,
            productOptionId,
            productId,
            productName,
            optionType,
            date,
            0L,
            0L,
            0,
            0,
            0,
            0,
            0,
            null,
            null
        );
    }

    public StockAnalyticsEntity addIncrease(int amount, String changeReason) {
        int orderCancelled = "ORDER_CANCELLED".equals(changeReason) ? this.orderCancelledCount + 1 : this.orderCancelledCount;
        int adminUpdate = "ADMIN_UPDATE".equals(changeReason) ? this.adminUpdateCount + 1 : this.adminUpdateCount;

        return new StockAnalyticsEntity(
            this.id,
            this.productOptionId,
            this.productId,
            this.productName,
            this.optionType,
            this.date,
            this.totalIncreased + amount,
            this.totalDecreased,
            this.increaseCount + 1,
            this.decreaseCount,
            this.orderCreatedCount,
            orderCancelled,
            adminUpdate,
            this.createdAt,
            this.updatedAt
        );
    }

    public StockAnalyticsEntity addDecrease(int amount, String changeReason) {
        int orderCreated = "ORDER_CREATED".equals(changeReason) ? this.orderCreatedCount + 1 : this.orderCreatedCount;
        int adminUpdate = "ADMIN_UPDATE".equals(changeReason) ? this.adminUpdateCount + 1 : this.adminUpdateCount;

        return new StockAnalyticsEntity(
            this.id,
            this.productOptionId,
            this.productId,
            this.productName,
            this.optionType,
            this.date,
            this.totalIncreased,
            this.totalDecreased + amount,
            this.increaseCount,
            this.decreaseCount + 1,
            orderCreated,
            this.orderCancelledCount,
            adminUpdate,
            this.createdAt,
            this.updatedAt
        );
    }
}
