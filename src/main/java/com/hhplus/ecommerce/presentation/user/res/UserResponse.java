package com.hhplus.ecommerce.presentation.user.res;

public record UserResponse(
        Long id,
        String username,
        Long point,
        String role,
        Long createdAt,
        Long updatedAt
) {
}