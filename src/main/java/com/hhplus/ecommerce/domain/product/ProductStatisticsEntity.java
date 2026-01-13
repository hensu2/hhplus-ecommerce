package com.hhplus.ecommerce.domain.product;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductStatisticsEntity {

    private Long productId;
    private Long viewCount;
    private Long salesCount;
    private Long updatedAt;

    public ProductStatisticsEntity increaseViewCount() {
        this.viewCount = this.viewCount + 1;
        this.updatedAt = System.currentTimeMillis();
        return this;
    }

    public ProductStatisticsEntity increaseSalesCount(Integer quantity) {
        this.salesCount = this.salesCount + quantity;
        this.updatedAt = System.currentTimeMillis();
        return this;
    }

    public long getPopularityScore() {
        // 조회수 * 1 + 판매량 * 10 (판매량에 더 높은 가중치)
        return (viewCount * 1) + (salesCount * 10);
    }
}
