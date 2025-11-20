package com.hhplus.ecommerce.presentation.user;

import com.hhplus.ecommerce.application.user.ChargePointUseCase;
import com.hhplus.ecommerce.application.user.GetPointHistoryUseCase;
import com.hhplus.ecommerce.application.user.GetPointUseCase;
import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.user.PointHistoryEntity;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.presentation.user.req.ChargePointRequest;
import com.hhplus.ecommerce.presentation.user.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포인트", description = "포인트 관리 API")
@RestController
@RequestMapping("/api/users/me/point")
@RequiredArgsConstructor
public class UserPointController {

    private final GetPointUseCase getPointUseCase;
    private final ChargePointUseCase chargePointUseCase;
    private final GetPointHistoryUseCase getPointHistoryUseCase;

    // 포인트 조회 (GET /api/users/me/point)
    @Operation(summary = "포인트 조회", description = "현재 사용자의 포인트 잔액을 조회합니다.")
    @GetMapping
    public PointResponse getPoint(@RequestParam Long userId) {
        UserEntity user = getPointUseCase.execute(userId);
        return new PointResponse(
                user.getId(),
                user.getUsername(),
                user.getPoint().intValue(),
                DateTimeUtils.toLocalDateTime(user.getUpdatedAt())
        );
    }

    // 포인트 충전 (POST /api/users/me/point/charge)
    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다.")
    @PostMapping("/charge")
    public ResponseEntity<ChargePointResponse> chargePoint(
            @RequestParam Long userId,
            @RequestBody ChargePointRequest request) {
        UserEntity savedUser = chargePointUseCase.execute(userId, request.amount().longValue());
        ChargePointResponse response = new ChargePointResponse(
                savedUser.getId(),
                request.amount(),
                savedUser.getPoint().intValue(),
                "EARN",
                DateTimeUtils.toLocalDateTime(savedUser.getUpdatedAt())
        );
        return ResponseEntity.ok(response);
    }

    // 포인트 사용 이력 조회 (GET /api/users/me/point/history)
    @Operation(summary = "포인트 사용 이력 조회", description = "포인트 충전/사용 이력을 조회합니다.")
    @GetMapping("/history")
    public PointHistoryResponse getPointHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<PointHistoryEntity> historyEntities = getPointHistoryUseCase.execute(userId);

        List<PointHistoryItemResponse> historyItems = historyEntities.stream()
                .map(this::toPointHistoryItemResponse)
                .toList();

        return new PointHistoryResponse(
                historyItems,
                historyItems.size(),
                1,
                size,
                page
        );
    }

    private PointHistoryItemResponse toPointHistoryItemResponse(PointHistoryEntity entity) {
        int amount = entity.getAmount().intValue();
        if ("USE".equals(entity.getTransactionType())) {
            amount = -amount;
        }

        return new PointHistoryItemResponse(
                entity.getId(),
                amount,
                entity.getTransactionType(),
                entity.getDescription(),
                DateTimeUtils.toLocalDateTime(entity.getCreatedAt())
        );
    }
}