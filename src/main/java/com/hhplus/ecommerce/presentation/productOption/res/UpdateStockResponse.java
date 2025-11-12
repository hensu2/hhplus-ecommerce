package com.hhplus.ecommerce.presentation.productOption.res;

public record UpdateStockResponse(
    Long optionId,
    String optionType,
    Long stock,
    Long additionalPrice
) {
}
