package com.hhplus.ecommerce.infrastructure.user.jpa;

import com.hhplus.ecommerce.domain.user.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdWithPessimisticLock(@Param("id") Long id);
}
