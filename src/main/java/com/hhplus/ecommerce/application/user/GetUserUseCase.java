package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.infrastructure.user.User;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class GetUserUseCase {

    private final UserRepository userRepository;

    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse execute(Long userId) {
        User.validateUserId(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return user.toUserResponse();
    }
}