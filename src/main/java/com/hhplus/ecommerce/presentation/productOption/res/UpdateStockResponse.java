package com.hhplus.ecommerce.presentation.productOption.res;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;

public record UpdateStockResponse(
    Long optionId,
    String optionType,
    Long stock,
    Long additionalPrice
) {
    public static UpdateStockResponse from(ProductOptionEntity entity) {
        return new UpdateStockResponse(
            entity.getId(),
            entity.getOptionType(),
            entity.getStock(),
            entity.getAdditionalPrice()
        );
    }
}
