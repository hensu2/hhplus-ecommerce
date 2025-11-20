package com.hhplus.ecommerce.presentation.productOption.res;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

public record StockOptionResponse(
        Long optionId,
        String optionType,
        Integer stock,
        Integer additionalPrice
) {
    public StockOptionResponse(ProductOptionEntity option) {
        this(
            option.getId(),
            option.getOptionType(),
            option.getStock().intValue(),
            option.getAdditionalPrice().intValue()
        );
    }
}