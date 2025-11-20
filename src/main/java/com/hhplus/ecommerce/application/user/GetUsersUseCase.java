package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetUsersUseCase {

    private final UserRepository userRepository;

    public List<UserEntity> execute() {
        return userRepository.findAll();
    }
}