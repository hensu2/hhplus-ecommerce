package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.PointHistoryEntity;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.PointHistoryRepository;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UsePointUseCase {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public UserEntity execute(Long userId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        String lockKey = "user:point:lock:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 (Pub/Sub 방식 - Redisson 기본)
            boolean acquired = lock.tryLock(10, 3, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("포인트 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 2. 트랜잭션 내에서 비즈니스 로직 수행
            return transactionTemplate.execute(status -> {
                // Get user and check balance
                UserEntity user = userRepository.getOrThrow(userId);

                if (user.getPoint() < amount) {
                    throw new IllegalArgumentException("포인트가 부족합니다. 현재 포인트: " + user.getPoint());
                }

                // Update point
                Long newPoint = user.getPoint() - amount;
                user.setPoint(newPoint);
                UserEntity savedUser = userRepository.save(user);

                // Create point history
                PointHistoryEntity history = PointHistoryEntity.create(
                        userId,
                        amount,
                        "USE",
                        "포인트 사용"
                );
                pointHistoryRepository.save(history);

                return savedUser;
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("포인트 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 3. 락 해제 (커밋 후)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}