package com.hhplus.ecommerce.presentation.productOption.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockResponse {
    private Long optionId;
    private String optionType;
    private Long stock;
    private Long additionalPrice;
}