package com.hhplus.ecommerce.presentation.user;

import com.hhplus.ecommerce.application.user.CreateUserUseCase;
import com.hhplus.ecommerce.application.user.GetUserUseCase;
import com.hhplus.ecommerce.application.user.GetUsersUseCase;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.presentation.user.req.CreateUserRequest;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserUseCase getUserUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final CreateUserUseCase createUserUseCase;

    // 사용자 조회 (GET /api/users/{id})
    @Operation(summary = "사용자 조회", description = "사용자 ID로 사용자 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserEntity user = getUserUseCase.execute(id);
        UserResponse response = toUserResponse(user);
        return ResponseEntity.ok(response);
    }

    // 전체 사용자 목록 조회 (GET /api/users)
    @Operation(summary = "사용자 목록 조회", description = "전체 사용자 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        List<UserEntity> users = getUsersUseCase.execute();
        List<UserResponse> responses = users.stream()
                .map(this::toUserResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // 사용자 생성 (POST /api/users)
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다.")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserEntity user = createUserUseCase.execute(request);
        UserResponse response = toUserResponse(user);
        return ResponseEntity.ok(response);
    }

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getPoint(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}