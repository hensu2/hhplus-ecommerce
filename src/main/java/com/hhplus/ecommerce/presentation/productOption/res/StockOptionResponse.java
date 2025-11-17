package com.hhplus.ecommerce.presentation.productOption.res;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

public record StockOptionResponse(
    Long optionId,
    String optionType,
    Long stock,
    Long additionalPrice
) {
    public static StockOptionResponse from(ProductOptionEntity entity) {
        return new StockOptionResponse(
            entity.getId(),
            entity.getOptionType(),
            entity.getStock(),
            entity.getAdditionalPrice()
        );
    }
}
