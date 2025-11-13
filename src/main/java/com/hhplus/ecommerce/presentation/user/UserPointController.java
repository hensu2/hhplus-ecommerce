package com.hhplus.ecommerce.presentation.user;

import com.hhplus.ecommerce.application.point.ChargeUserPointUseCase;
import com.hhplus.ecommerce.application.point.GetUserPointHistoryUseCase;
import com.hhplus.ecommerce.application.point.GetUserPointUseCase;
import com.hhplus.ecommerce.presentation.user.req.ChargePointRequest;
import com.hhplus.ecommerce.presentation.user.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "포인트", description = "포인트 관리 API")
@RestController
@RequestMapping("/api/users/{userId}/point")
@RequiredArgsConstructor
public class UserPointController {

    private final GetUserPointUseCase getUserPointUseCase;
    private final ChargeUserPointUseCase chargeUserPointUseCase;
    private final GetUserPointHistoryUseCase getUserPointHistoryUseCase;

    // 포인트 조회 (GET /api/users/{userId}/point)
    @Operation(summary = "포인트 조회", description = "현재 사용자의 포인트 잔액을 조회합니다.")
    @GetMapping
    public PointResponse getPoint(@PathVariable Long userId) {
        return getUserPointUseCase.execute(userId);
    }

    // 포인트 충전 (POST /api/users/{userId}/point/charge)
    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다.")
    @PostMapping("/charge")
    public ResponseEntity<ChargePointResponse> chargePoint(
            @PathVariable Long userId,
            @RequestBody ChargePointRequest request) {
        ChargePointResponse response = chargeUserPointUseCase.execute(userId, request.amount());
        return ResponseEntity.ok(response);
    }

    // 포인트 사용 이력 조회 (GET /api/users/{userId}/point/history)
    @Operation(summary = "포인트 사용 이력 조회", description = "포인트 충전/사용 이력을 조회합니다.")
    @GetMapping("/history")
    public PointHistoryResponse getPointHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return getUserPointHistoryUseCase.execute(userId, page, size);
    }
}