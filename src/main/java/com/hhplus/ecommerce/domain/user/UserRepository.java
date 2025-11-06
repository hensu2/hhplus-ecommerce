package com.hhplus.ecommerce.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<UserEntity> findById(Long id);
    List<UserEntity> findAll();
    UserEntity save(UserEntity user);
}