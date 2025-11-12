package com.hhplus.ecommerce.presentation.productOption.res;

import java.util.List;

public record ProductStockResponse(
    Long productId,
    String productName,
    List<StockOptionResponse> options
) {
}
