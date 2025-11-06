package com.hhplus.ecommerce.domain.user;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;

public record UserEntity(
    long id,
    String username,
    long point,
    String role,
    long createdAt,
    long updatedAt
) {

    public static void validateUserId(Long userId) {
        if (userId == null) {
            throw new InvalidInputException("User ID cannot be null");
        }
        if (userId <= 0) {
            throw new InvalidInputException("User ID must be greater than 0");
        }
    }

    public static UserEntity create(String username, long point, String role) {
        validateUsername(username);
        validatePoint(point);
        validateRole(role);
        return new UserEntity(0L, username, point, role, 0L, 0L);
    }

    private static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidInputException("사용자 이름은 필수입니다.");
        }
    }

    private static void validatePoint(long point) {
        if (point < 0) {
            throw new InvalidInputException("포인트는 0 이상이어야 합니다.");
        }
    }

    private static void validateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new InvalidInputException("역할은 필수입니다.");
        }
    }

    public UserResponse toUserResponse() {
        return new UserResponse(id, username, point, role, createdAt, updatedAt);
    }
}