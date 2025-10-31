package com.example.demo.controller.cart;

import com.example.demo.dto.request.AddToCartRequest;
import com.example.demo.dto.request.UpdateCartRequest;
import com.example.demo.dto.response.CartItemResponse;
import com.example.demo.dto.response.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Cart", description = "장바구니 API")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Operation(
            summary = "장바구니 조회",
            description = "현재 사용자의 장바구니 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장바구니 조회 성공",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId
    ) {
        // Mock 데이터 생성
        List<CartItemResponse> items = new ArrayList<>();

        CartItemResponse item1 = new CartItemResponse(
                1L, 1L, "상품명", 1L, "색상:블랙",
                2, 10000, 20000, 100
        );
        item1.setAddedAt(LocalDateTime.now());
        items.add(item1);

        CartResponse response = new CartResponse(items, 20000);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "장바구니 추가",
            description = "상품을 장바구니에 추가합니다. 재고를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "장바구니 추가 성공",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "재고 부족",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<CartItemResponse> addToCart(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "장바구니 추가 요청",
                    required = true
            )
            @RequestBody AddToCartRequest request
    ) {
        // Mock 데이터 생성
        CartItemResponse response = new CartItemResponse(
                1L,
                request.getProductId(),
                "상품명",
                request.getOptionId(),
                "색상:블랙",
                request.getQuantity(),
                10000,
                10000 * request.getQuantity(),
                100
        );
        response.setAddedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "장바구니 수량 변경",
            description = "장바구니 아이템의 수량을 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수량 변경 성공",
                    content = @Content(schema = @Schema(implementation = CartItemResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<CartItemResponse> updateCartItem(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "장바구니 아이템 ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수량 변경 요청",
                    required = true
            )
            @RequestBody UpdateCartRequest request
    ) {
        // Mock 데이터 생성
        CartItemResponse response = new CartItemResponse(
                id, 1L, "상품명", 1L, "색상:블랙",
                request.getQuantity(), 10000, 10000 * request.getQuantity(), 100
        );
        response.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "장바구니 삭제",
            description = "장바구니에서 특정 아이템을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자 인증 필요"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCartItem(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "장바구니 아이템 ID", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "에러 응답")
    @Getter
    @Setter
    private static class ErrorResponse {
        @Schema(description = "에러 코드", example = "OUT_OF_STOCK")
        private String error;

        @Schema(description = "에러 메시지", example = "재고가 부족합니다.")
        private String message;

        @Schema(description = "사용 가능한 재고", example = "1")
        private Integer availableStock;

    }
}