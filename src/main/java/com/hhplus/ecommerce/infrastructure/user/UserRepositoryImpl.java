package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserTable userTable;

    public UserRepositoryImpl(UserTable userTable) {
        this.userTable = userTable;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userTable.findById(id);
    }
}