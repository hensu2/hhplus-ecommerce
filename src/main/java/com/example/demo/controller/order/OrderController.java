package com.example.demo.controller.order;

import com.example.demo.dto.request.CancelOrderRequest;
import com.example.demo.dto.request.CreateOrderRequest;
import com.example.demo.dto.response.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Operation(
            summary = "주문 생성",
            description = "장바구니의 상품들로 주문을 생성합니다. 재고 확인 및 차감, 쿠폰 적용, 포인트 차감이 트랜잭션으로 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "주문 생성 성공",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "재고 부족, 포인트 부족, 쿠폰 무효",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "주문 생성 요청",
                    required = true
            )
            @RequestBody CreateOrderRequest request
    ) {
        // Mock 데이터 생성
        List<OrderItemResponse> items = new ArrayList<>();
        items.add(new OrderItemResponse(
                1L, "상품명", 1L, "색상:블랙", 2, 10000, 20000
        ));

        OrderResponse response = new OrderResponse(
                1L, 1L, "PENDING", items,
                50000, 5000, 5000, 40000,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "주문 목록 조회",
            description = "사용자의 주문 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "주문 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<PageResponse<OrderListResponse>> getOrders(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "페이지 번호 (default: 0)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기 (default: 20)")
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "주문 상태 (PENDING, PAID, CANCELLED)")
            @RequestParam(required = false) String status
    ) {
        // Mock 데이터 생성
        List<OrderListResponse> content = new ArrayList<>();
        content.add(new OrderListResponse(
                1L, "PAID", 50000, 40000, 3, LocalDateTime.now()
        ));

        PageResponse<OrderListResponse> response = new PageResponse<>(
                content, 10L, 1, size, page
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "특정 주문의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "주문 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "주문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long orderId
    ) {
        // Mock 데이터 생성
        List<OrderItemResponse> items = new ArrayList<>();
        items.add(new OrderItemResponse(
                1L, "상품명", 1L, "색상:블랙", 2, 10000, 20000
        ));

        OrderCouponResponse coupon = new OrderCouponResponse(
                "신규 회원 10% 할인 쿠폰", 5000
        );

        OrderPaymentResponse payment = new OrderPaymentResponse(
                1L, 40000, LocalDateTime.now().plusSeconds(10)
        );

        OrderDetailResponse response = new OrderDetailResponse(
                orderId, 1L, "PAID", items,
                50000, 5000, 5000, 40000,
                coupon, payment, LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "주문 취소",
            description = "주문을 취소합니다. 재고, 포인트, 쿠폰이 복구됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "주문 취소 성공",
                    content = @Content(schema = @Schema(implementation = CancelOrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "취소 불가 상태",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long orderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "취소 요청",
                    required = true
            )
            @RequestBody CancelOrderRequest request
    ) {
        // Mock 데이터 생성
        CancelOrderResponse response = new CancelOrderResponse(
                orderId, "CANCELLED", 40000, 40000, true, LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @Schema(description = "에러 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorResponse {
        @Schema(description = "에러 코드", example = "INSUFFICIENT_STOCK")
        private String error;

        @Schema(description = "에러 메시지", example = "상품 '상품명'의 재고가 부족합니다.")
        private String message;

        @Schema(description = "상품 ID", example = "1")
        private Long productId;

        @Schema(description = "사용 가능한 재고", example = "1")
        private Integer availableStock;

        @Schema(description = "현재 잔액", example = "3000")
        private Integer currentBalance;

        @Schema(description = "필요 금액", example = "40000")
        private Integer requiredAmount;
    }
}