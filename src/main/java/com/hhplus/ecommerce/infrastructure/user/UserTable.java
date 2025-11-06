package com.hhplus.ecommerce.infrastructure.user;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class UserTable {
    private final ConcurrentHashMap<Long, User> table = new ConcurrentHashMap<>();
    private long cursor = 4;

    public UserTable() {
        // 초기 데이터
        long timestamp = System.currentTimeMillis();
        table.put(1L, new User(1L, "user123", 50000L, "USER", timestamp, timestamp));
        table.put(2L, new User(2L, "admin", 100000L, "ADMIN", timestamp, timestamp));
        table.put(3L, new User(3L, "testuser", 30000L, "USER", timestamp, timestamp));
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(table.get(id));
    }

    public List<User> findAll() {
        return List.copyOf(table.values());
    }

    public User save(User user) {
        long id = ++cursor;
        long timestamp = System.currentTimeMillis();
        User newUser = new User(id, user.username(), user.point(), user.role(), timestamp, timestamp);
        table.put(id, newUser);
        return newUser;
    }
}