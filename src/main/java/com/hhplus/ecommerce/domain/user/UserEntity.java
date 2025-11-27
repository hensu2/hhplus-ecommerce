package com.hhplus.ecommerce.domain.user;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Long point;

    @Column(nullable = false)
    private String role;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public static void validateUserId(Long userId) {
        if (userId == null) {
            throw new InvalidInputException("User ID cannot be null");
        }
        if (userId <= 0) {
            throw new InvalidInputException("User ID must be greater than 0");
        }
    }

    public static UserEntity create(String username, Long point, String role) {
        validateUsername(username);
        validatePoint(point);
        validateRole(role);
        long now = System.currentTimeMillis();
        return new UserEntity(null, username, point, role, now, now);
    }

    private static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidInputException("사용자 이름은 필수입니다.");
        }
    }

    private static void validatePoint(Long point) {
        if (point == null || point < 0) {
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

    public void setPoint(Long point) {
        this.point = point;
    }
}
