package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.req.CreateUserRequest;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {

    private final UserRepository userRepository;

    public UserResponse execute(CreateUserRequest request) {
        // User 객체 생성 및 검증 (id와 timestamp는 save 시 자동 생성)
        UserEntity user = UserEntity.create(request.username(), request.point(), request.role());

        // 저장
        UserEntity savedUser = userRepository.save(user);

        // 응답 반환
        return savedUser.toUserResponse();
    }
}
