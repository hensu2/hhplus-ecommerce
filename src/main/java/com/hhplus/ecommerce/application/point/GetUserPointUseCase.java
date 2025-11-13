package com.hhplus.ecommerce.application.point;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.res.PointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class GetUserPointUseCase {

    private final UserRepository userRepository;

    public PointResponse execute(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return new PointResponse(
            user.getId(),
            user.getUsername(),
            user.getPoint().intValue(),
            formatTimestamp(System.currentTimeMillis())
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
