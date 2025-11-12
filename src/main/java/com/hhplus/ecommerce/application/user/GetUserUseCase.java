package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.common.exception.UserNotFoundException;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository userRepository;

    public UserResponse execute(Long userId) {
        UserEntity.validateUserId(userId);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return user.toUserResponse();
    }
}