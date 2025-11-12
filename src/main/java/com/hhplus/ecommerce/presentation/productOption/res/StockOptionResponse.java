package com.hhplus.ecommerce.presentation.productOption.res;

public record StockOptionResponse(
    Long optionId,
    String optionType,
    Integer stock,
    Integer additionalPrice
) {
}
