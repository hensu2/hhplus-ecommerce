package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.infrastructure.user.User;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetUsersUseCase {

    private final UserRepository userRepository;

    public GetUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> execute() {
        List<User> users = userRepository.findAll();

        return users.stream()
            .map(User::toUserResponse)
            .toList();
    }
}