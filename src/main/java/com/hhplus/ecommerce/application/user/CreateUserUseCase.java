package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.req.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {

    private final UserRepository userRepository;

    public UserEntity execute(CreateUserRequest request) {
        UserEntity user = UserEntity.create(request.username(), request.point(), request.role());
        return userRepository.save(user);
    }
}
