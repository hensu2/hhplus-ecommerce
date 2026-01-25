package com.hhplus.ecommerce.domain.salesRanking;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesRankingItem {
    private long rank;
    private Long productId;
    private Long salesCount;
}