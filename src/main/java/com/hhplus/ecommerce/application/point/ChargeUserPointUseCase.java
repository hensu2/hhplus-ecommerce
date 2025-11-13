package com.hhplus.ecommerce.application.point;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.PointHistoryRepository;
import com.hhplus.ecommerce.domain.point.TransactionType;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.res.ChargePointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ChargeUserPointUseCase {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public ChargePointResponse execute(Long userId, Integer amount) {
        if (amount < 1000) {
            throw new IllegalArgumentException("충전 금액은 1,000원 이상이어야 합니다.");
        }

        // 1. 사용자 조회 및 포인트 충전
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.chargePoint(amount.longValue());
        UserEntity updatedUser = userRepository.save(user);

        // 2. 포인트 히스토리 기록
        PointHistoryEntity history = PointHistoryEntity.createEarn(
            updatedUser,
            amount.longValue(),
            "포인트 충전"
        );
        pointHistoryRepository.save(history);

        // 3. 응답 생성
        long now = System.currentTimeMillis();
        return new ChargePointResponse(
            updatedUser.getId(),
            amount,
            updatedUser.getPoint().intValue(),
            "EARN",
            formatTimestamp(now)
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
