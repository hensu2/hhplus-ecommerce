package com.hhplus.ecommerce.presentation.user;

import com.hhplus.ecommerce.application.user.GetUserUseCase;
import com.hhplus.ecommerce.infrastructure.user.User;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "사용자", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetUserUseCase getUserUseCase;

    public UserController(GetUserUseCase getUserUseCase) {
        this.getUserUseCase = getUserUseCase;
    }

    // 간단한 메모리 데이터
    private static final Map<Long, User> USERS = new HashMap<>();

    static {
        // 초기 데이터
        long timestamp = System.currentTimeMillis();
        USERS.put(1L, new User(1L, "user123", 50000L, "USER", timestamp, timestamp));
        USERS.put(2L, new User(2L, "admin", 100000L, "ADMIN", timestamp, timestamp));
        USERS.put(3L, new User(3L, "testuser", 30000L, "USER", timestamp, timestamp));
    }

    // 사용자 조회 (GET /api/users/{id})
    @Operation(summary = "사용자 조회", description = "사용자 ID로 사용자 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = getUserUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    // 전체 사용자 목록 조회 (GET /api/users)
    @Operation(summary = "사용자 목록 조회", description = "전체 사용자 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        List<UserResponse> userResponses = USERS.values().stream()
            .map(User::toUserResponse)
            .toList();

        return ResponseEntity.ok(userResponses);
    }
}