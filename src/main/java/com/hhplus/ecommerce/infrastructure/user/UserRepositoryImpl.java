package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.memory.UserTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserTable userTable;

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userTable.findById(id);
    }

    @Override
    public List<UserEntity> findAll() {
        return userTable.findAll();
    }

    @Override
    public UserEntity save(UserEntity user) {
        return userTable.save(user);
    }
}