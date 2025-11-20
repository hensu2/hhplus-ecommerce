package com.hhplus.ecommerce.presentation.user.res;

public record PointResponse(
        Long userId,
        String username,
        Integer point,
        String updatedAt
) {
}
