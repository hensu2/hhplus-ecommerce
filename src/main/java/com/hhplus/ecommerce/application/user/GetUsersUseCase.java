package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetUsersUseCase {

    private final UserRepository userRepository;

    public List<UserResponse> execute() {
        List<UserEntity> users = userRepository.findAll();

        return users.stream()
            .map(UserEntity::toUserResponse)
            .toList();
    }
}