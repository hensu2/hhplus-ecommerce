package com.hhplus.ecommerce.infrastructure.user.jpa;

import com.hhplus.ecommerce.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
}
