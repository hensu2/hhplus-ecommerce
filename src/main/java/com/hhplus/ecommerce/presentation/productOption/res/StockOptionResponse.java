package com.hhplus.ecommerce.presentation.productOption.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockOptionResponse {
    private Long optionId;
    private String optionType;
    private Integer stock;
    private Integer additionalPrice;
}