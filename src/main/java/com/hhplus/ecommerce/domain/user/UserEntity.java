package com.hhplus.ecommerce.domain.user;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "USERS", indexes = {
    @Index(name = "idx_username", columnList = "username")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "point", nullable = false)
    private Long point = 0L;

    @Column(name = "role", nullable = false, length = 20)
    private String role = "USER";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    private UserEntity(Long id, String username, Long point, String role) {
        this.id = id;
        this.username = username;
        this.point = point;
        this.role = role;
    }

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
        return new UserEntity(null, username, point, role);
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
        // LocalDateTime을 timestamp로 변환
        long createdAtTimestamp = createdAt != null ?
            createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;
        long updatedAtTimestamp = updatedAt != null ?
            updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;

        return new UserResponse(id, username, point, role, createdAtTimestamp, updatedAtTimestamp);
    }

    // 포인트 충전
    public void chargePoint(long amount) {
        if (amount <= 0) {
            throw new InvalidInputException("충전 금액은 0보다 커야 합니다.");
        }
        this.point += amount;
    }

    // 포인트 사용
    public void usePoint(long amount) {
        if (amount <= 0) {
            throw new InvalidInputException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            throw new InvalidInputException("포인트가 부족합니다.");
        }
        this.point -= amount;
    }
}