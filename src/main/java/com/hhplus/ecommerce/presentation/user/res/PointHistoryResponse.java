package com.hhplus.ecommerce.presentation.user.res;

import java.util.List;

public record PointHistoryResponse(
        List<PointHistoryItemResponse> content,
        Integer totalElements,
        Integer totalPages,
        Integer size,
        Integer number
) {
}
