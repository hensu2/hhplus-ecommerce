package com.hhplus.ecommerce.presentation.product.res;

import java.util.List;

public record PopularProductListResponse(
    List<PopularProductResponse> products,
    String period,
    String generatedAt
) {
}
