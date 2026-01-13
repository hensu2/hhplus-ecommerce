package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.common.exception.UserNotFoundException;
import com.hhplus.ecommerce.domain.user.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<UserEntity> findById(Long id);
    Optional<UserEntity> findByIdWithPessimisticLock(Long id);
    List<UserEntity> findAll();
    UserEntity save(UserEntity user);

    default UserEntity getOrThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + id));
    }

    default UserEntity getOrThrowWithLock(Long id) {
        return findByIdWithPessimisticLock(id)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + id));
    }
}