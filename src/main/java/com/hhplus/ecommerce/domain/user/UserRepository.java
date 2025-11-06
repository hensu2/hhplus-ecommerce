package com.hhplus.ecommerce.domain.user;

import com.hhplus.ecommerce.infrastructure.user.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
}