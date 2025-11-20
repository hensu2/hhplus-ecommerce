package com.hhplus.ecommerce.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "product_statistics")
public class ProductStatisticsEntity {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public ProductStatisticsEntity increaseViewCount() {
        this.viewCount = this.viewCount + 1;
        // updatedAt은 @LastModifiedDate가 자동 관리
        return this;
    }

    public ProductStatisticsEntity increaseSalesCount(Integer quantity) {
        this.salesCount = this.salesCount + quantity;
        // updatedAt은 @LastModifiedDate가 자동 관리
        return this;
    }

    public long getPopularityScore() {
        // 조회수 * 1 + 판매량 * 10 (판매량에 더 높은 가중치)
        return (viewCount * 1) + (salesCount * 10);
    }
}
