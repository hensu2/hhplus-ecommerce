package com.hhplus.ecommerce.presentation.user;

import com.hhplus.ecommerce.presentation.user.req.ChargePointRequest;
import com.hhplus.ecommerce.presentation.user.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Tag(name = "포인트", description = "포인트 관리 API")
@RestController
@RequestMapping("/api/users/me/point")
public class UserPointController {

    private static int currentPoint = 50000;
    private static final AtomicLong HISTORY_ID_GENERATOR = new AtomicLong(3);
    private static final List<PointHistoryItemResponse> POINT_HISTORY = new ArrayList<>();

    static {
        // 초기 데이터
        POINT_HISTORY.add(new PointHistoryItemResponse(
            1L, 10000, "EARN", "포인트 충전", "2024-10-30T00:00:00"
        ));
        POINT_HISTORY.add(new PointHistoryItemResponse(
            2L, -5000, "USE", "주문 결제", "2024-10-29T00:00:00"
        ));
    }

    // 포인트 조회 (GET /api/users/me/point)
    @Operation(summary = "포인트 조회", description = "현재 사용자의 포인트 잔액을 조회합니다.")
    @GetMapping
    public PointResponse getPoint() {
        return new PointResponse(
            1L,
            "user123",
            currentPoint,
            LocalDateTime.now().toString()
        );
    }

    // 포인트 충전 (POST /api/users/me/point/charge)
    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다.")
    @PostMapping("/charge")
    public ResponseEntity<ChargePointResponse> chargePoint(@RequestBody ChargePointRequest request) {
        if (request.amount() < 1000) {
            throw new RuntimeException("충전 금액은 1,000원 이상이어야 합니다.");
        }

        currentPoint += request.amount();

        POINT_HISTORY.add(0, new PointHistoryItemResponse(
            HISTORY_ID_GENERATOR.getAndIncrement(),
            request.amount(),
            "EARN",
            "포인트 충전",
            LocalDateTime.now().toString()
        ));

        return ResponseEntity.ok(new ChargePointResponse(
            1L,
            request.amount(),
            currentPoint,
            "EARN",
            LocalDateTime.now().toString()
        ));
    }

    // 포인트 사용 이력 조회 (GET /api/users/me/point/history)
    @Operation(summary = "포인트 사용 이력 조회", description = "포인트 충전/사용 이력을 조회합니다.")
    @GetMapping("/history")
    public PointHistoryResponse getPointHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return new PointHistoryResponse(
            POINT_HISTORY,
            POINT_HISTORY.size(),
            1,
            size,
            page
        );
    }
}