package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.presentation.user.req.CreateUserRequest;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;

public record User(
    long id,
    String username,
    long point,
    String role,
    long createdAt,
    long updatedAt
) {

    public static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than 0");
        }
    }

    public UserResponse toUserResponse() {
        return new UserResponse(id, username, point, role, createdAt, updatedAt);
    }
}