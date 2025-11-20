package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository userRepository;

    public UserEntity execute(Long userId) {
        UserEntity.validateUserId(userId);
        return userRepository.getOrThrow(userId);
    }
}