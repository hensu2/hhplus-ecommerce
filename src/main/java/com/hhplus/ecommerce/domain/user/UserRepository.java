package com.hhplus.ecommerce.domain.user;

import com.hhplus.ecommerce.infrastructure.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    List<User> findAll();
    User save(User user);
}