package com.hhplus.ecommerce.domain.product;

public record ProductStatisticsEntity(
    long productId,
    long viewCount,
    long salesCount,
    long updatedAt
) {
    public ProductStatisticsEntity increaseViewCount() {
        return new ProductStatisticsEntity(
            this.productId,
            this.viewCount + 1,
            this.salesCount,
            System.currentTimeMillis()
        );
    }

    public ProductStatisticsEntity increaseSalesCount(int quantity) {
        return new ProductStatisticsEntity(
            this.productId,
            this.viewCount,
            this.salesCount + quantity,
            System.currentTimeMillis()
        );
    }

    public long getPopularityScore() {
        // 조회수 * 1 + 판매량 * 10 (판매량에 더 높은 가중치)
        return (viewCount * 1) + (salesCount * 10);
    }
}
