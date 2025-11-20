package com.hhplus.ecommerce.presentation.product.res;

import java.util.List;

public record ProductListResponse(
        List<ProductResponse> content,
        Integer totalElements,
        Integer totalPages,
        Integer size,
        Integer number
) {
}
