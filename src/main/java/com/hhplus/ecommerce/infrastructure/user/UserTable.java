package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserTable {
    private final ConcurrentHashMap<Long, UserEntity> table = new ConcurrentHashMap<>();
    private long cursor = 4;

    public UserTable() {
        // 초기 데이터
        long timestamp = System.currentTimeMillis();
        table.put(1L, new UserEntity(1L, "user123", 50000L, "USER", timestamp, timestamp));
        table.put(2L, new UserEntity(2L, "admin", 100000L, "ADMIN", timestamp, timestamp));
        table.put(3L, new UserEntity(3L, "testuser", 30000L, "USER", timestamp, timestamp));
    }

    public Optional<UserEntity> findById(Long id) {
        return Optional.ofNullable(table.get(id));
    }

    public List<UserEntity> findAll() {
        return List.copyOf(table.values());
    }

    public UserEntity save(UserEntity user) {
        long id = ++cursor;
        long timestamp = System.currentTimeMillis();
        UserEntity newUser = new UserEntity(id, user.username(), user.point(), user.role(), timestamp, timestamp);
        table.put(id, newUser);
        return newUser;
    }
}