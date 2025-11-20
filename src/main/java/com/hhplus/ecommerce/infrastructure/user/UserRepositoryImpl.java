package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public List<UserEntity> findAll() {
        return userJpaRepository.findAll();
    }

    @Override
    public UserEntity save(UserEntity user) {
        return userJpaRepository.save(user);
    }
}
