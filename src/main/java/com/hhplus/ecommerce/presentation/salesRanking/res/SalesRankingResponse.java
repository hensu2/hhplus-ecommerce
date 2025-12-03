package com.hhplus.ecommerce.presentation.salesRanking.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SalesRankingResponse {
    private String type;
    private String period;
    private List<SalesRankingDto> rankings;
    private int page;
    private int size;
    private long totalCount;
}