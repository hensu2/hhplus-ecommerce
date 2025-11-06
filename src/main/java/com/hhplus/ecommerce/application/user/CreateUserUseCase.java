package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.infrastructure.user.User;
import com.hhplus.ecommerce.presentation.user.req.CreateUserRequest;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class CreateUserUseCase {

    private final UserRepository userRepository;

    public CreateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse execute(CreateUserRequest request) {
        // 입력 값 검증
        validateRequest(request);

        // User 객체 생성 (id와 timestamp는 save 시 자동 생성)
        User user = new User(0L, request.getUsername(), request.getPoint(), request.getRole(), 0L, 0L);

        // 저장
        User savedUser = userRepository.save(user);

        // 응답 반환
        return savedUser.toUserResponse();
    }

    private void validateRequest(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 이름은 필수입니다.");
        }
        if (request.getPoint() == null || request.getPoint() < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수입니다.");
        }
    }
}
