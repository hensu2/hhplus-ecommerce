package com.example.demo.controller.point;

import com.example.demo.dto.request.ChargePointRequest;
import com.example.demo.dto.response.ChargePointResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.PointHistoryResponse;
import com.example.demo.dto.response.PointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Point", description = "포인트 API")
@RestController
@RequestMapping("/api/users/me/point")
public class PointController {

    @Operation(
            summary = "포인트 조회",
            description = "현재 사용자의 포인트 잔액을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "포인트 조회 성공",
                    content = @Content(schema = @Schema(implementation = PointResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<PointResponse> getPoint(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId
    ) {
        // Mock 데이터 생성
        PointResponse response = new PointResponse(
                1L,
                "user123",
                50000,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "포인트 충전",
            description = "사용자의 포인트를 충전합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "충전 성공",
                    content = @Content(schema = @Schema(implementation = ChargePointResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 충전 금액",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/charge")
    public ResponseEntity<ChargePointResponse> chargePoint(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "충전 요청",
                    required = true
            )
            @RequestBody ChargePointRequest request
    ) {
        // Mock 데이터 생성
        ChargePointResponse response = new ChargePointResponse(
                1L,
                request.getAmount(),
                50000 + request.getAmount(),
                "EARN",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "포인트 사용 이력 조회",
            description = "포인트 충전/사용 이력을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이력 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/history")
    public ResponseEntity<PageResponse<PointHistoryResponse>> getPointHistory(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "페이지 번호 (default: 0)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기 (default: 20)")
            @RequestParam(defaultValue = "20") Integer size
    ) {
        // Mock 데이터 생성
        List<PointHistoryResponse> content = new ArrayList<>();
        content.add(new PointHistoryResponse(
                1L, 10000, "EARN", "포인트 충전", LocalDateTime.now()
        ));
        content.add(new PointHistoryResponse(
                2L, -5000, "USE", "주문 결제", LocalDateTime.now().minusDays(1)
        ));

        PageResponse<PointHistoryResponse> response = new PageResponse<>(
                content, 10L, 1, size, page
        );

        return ResponseEntity.ok(response);
    }

    @Schema(description = "에러 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorResponse {
        @Schema(description = "에러 코드", example = "INVALID_AMOUNT")
        private String error;

        @Schema(description = "에러 메시지", example = "충전 금액은 1,000원 이상이어야 합니다.")
        private String message;
    }
}