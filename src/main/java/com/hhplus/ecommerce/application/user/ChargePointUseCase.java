package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.PointHistoryEntity;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.PointHistoryRepository;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChargePointUseCase {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public UserEntity execute(Long userId, Long amount) {
        if (amount == null || amount < 1000) {
            throw new IllegalArgumentException("충전 금액은 1,000원 이상이어야 합니다.");
        }

        // Get user and update point
        UserEntity user = userRepository.getOrThrow(userId);
        Long newPoint = user.getPoint() + amount;
        user.setPoint(newPoint);
        UserEntity savedUser = userRepository.save(user);

        // Create point history
        PointHistoryEntity history = PointHistoryEntity.create(
            userId,
            amount,
            "EARN",
            "포인트 충전"
        );
        pointHistoryRepository.save(history);

        return savedUser;
    }
}
