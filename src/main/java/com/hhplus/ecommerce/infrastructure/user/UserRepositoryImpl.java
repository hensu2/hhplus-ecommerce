package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.memory.UserTable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserTable userTable;

    public UserRepositoryImpl(UserTable userTable) {
        this.userTable = userTable;
    }

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