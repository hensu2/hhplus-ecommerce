package com.hhplus.ecommerce.domain.productOption;

import com.hhplus.ecommerce.presentation.productOption.res.StockOptionResponse;

public record ProductOptionEntity(
    long id,
    long productId,
    String optionType,
    long additionalPrice,
    long stock,
    long createdAt,
    long updatedAt
) {
    public StockOptionResponse toStockOptionResponse() {
        return new StockOptionResponse(id, optionType, (int) stock, (int) additionalPrice);
    }
}
