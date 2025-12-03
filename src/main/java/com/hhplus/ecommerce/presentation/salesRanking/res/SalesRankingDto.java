package com.hhplus.ecommerce.presentation.salesRanking.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesRankingDto {
    private long rank;
    private Long productId;
    private String productName;
    private Long price;
    private Long salesCount;
}