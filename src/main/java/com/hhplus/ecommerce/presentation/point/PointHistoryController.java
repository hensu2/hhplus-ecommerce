package com.hhplus.ecommerce.presentation.point;

import com.hhplus.ecommerce.application.point.GetPointHistoryUseCase;
import com.hhplus.ecommerce.application.point.RecordPointHistoryUseCase;
import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.TransactionType;
import com.hhplus.ecommerce.presentation.point.req.RecordPointHistoryRequest;
import com.hhplus.ecommerce.presentation.point.res.PointHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Point History", description = "포인트 내역 API")
@RestController
@RequestMapping("/api/point-history")
@RequiredArgsConstructor
public class PointHistoryController {

    private final GetPointHistoryUseCase getPointHistoryUseCase;
    private final RecordPointHistoryUseCase recordPointHistoryUseCase;

    @Operation(summary = "사용자 포인트 내역 조회", description = "특정 사용자의 포인트 내역을 최신순으로 조회합니다")
    @GetMapping("/user/{userId}")
    public List<PointHistoryResponse> getPointHistory(@PathVariable Long userId) {
        List<PointHistoryEntity> histories = getPointHistoryUseCase.execute(userId);
        return histories.stream()
                .map(PointHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Operation(summary = "포인트 내역 기록", description = "포인트 적립/사용/환불 내역을 기록합니다")
    @PostMapping
    public PointHistoryResponse recordPointHistory(@RequestBody RecordPointHistoryRequest request) {
        TransactionType transactionType = TransactionType.valueOf(request.transactionType());
        PointHistoryEntity history = recordPointHistoryUseCase.execute(
                request.userId(),
                request.amount(),
                transactionType,
                request.description()
        );
        return PointHistoryResponse.from(history);
    }
}