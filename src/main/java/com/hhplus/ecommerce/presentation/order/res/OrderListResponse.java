package com.hhplus.ecommerce.presentation.order.res;

import java.util.List;

public record OrderListResponse(
        List<OrderListItemResponse> content,
        Integer totalElements,
        Integer totalPages,
        Integer size,
        Integer number
) {
}
