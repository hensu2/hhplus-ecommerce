package com.hhplus.ecommerce.presentation.productOption.res;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

public record UpdateStockResponse(
        Long optionId,
        String optionType,
        Long stock,
        Long additionalPrice
) {
    public UpdateStockResponse(ProductOptionEntity option) {
        this(
            option.getId(),
            option.getOptionType(),
            option.getStock(),
            option.getAdditionalPrice()
        );
    }
}