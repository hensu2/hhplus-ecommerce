package com.hhplus.ecommerce.presentation.user.req;

public record CreateUserRequest(
        String username,
        Long point,
        String role
) {
}
