package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
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
        // User 객체 생성 및 검증 (id와 timestamp는 save 시 자동 생성)
        UserEntity user = UserEntity.create(request.getUsername(), request.getPoint(), request.getRole());

        // 저장
        UserEntity savedUser = userRepository.save(user);

        // 응답 반환
        return savedUser.toUserResponse();
    }
}
