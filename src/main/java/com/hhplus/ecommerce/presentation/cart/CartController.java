package com.hhplus.ecommerce.presentation.cart;

import com.hhplus.ecommerce.common.dto.ErrorResponse;
import com.hhplus.ecommerce.presentation.cart.req.AddToCartRequest;
import com.hhplus.ecommerce.presentation.cart.req.UpdateCartItemRequest;
import com.hhplus.ecommerce.presentation.cart.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Tag(name = "장바구니", description = "장바구니 관리 API")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final AtomicLong CART_ID_GENERATOR = new AtomicLong(2);
    private static final Map<Long, CartItemResponse> CART_ITEMS = new LinkedHashMap<>();

    static {
        // 초기 데이터
        CART_ITEMS.put(1L, new CartItemResponse(
            1L, 1L, "노트북", 1L, "색상:블랙", 2, 890000, 1780000, 100
        ));
    }

    // 장바구니 조회 (GET /api/cart)
    @Operation(summary = "장바구니 조회", description = "현재 사용자의 장바구니 목록을 조회합니다.")
    @GetMapping
    public CartResponse getCart() {
        List<CartItemResponse> items = new ArrayList<>(CART_ITEMS.values());
        int totalAmount = items.stream()
                .mapToInt(CartItemResponse::getTotalPrice)
                .sum();

        return new CartResponse(items, totalAmount);
    }

    // 장바구니 추가 (POST /api/cart)
    @Operation(summary = "장바구니 추가", description = "상품을 장바구니에 추가합니다. 재고를 확인합니다.")
    @PostMapping
    public ResponseEntity<AddCartItemResponse> addToCart(@RequestBody AddToCartRequest request) {
        // Mock 재고 확인
        int availableStock = 100;
        if (request.getQuantity() > availableStock) {
            throw new RuntimeException("재고가 부족합니다.");
        }

        Long cartItemId = CART_ID_GENERATOR.getAndIncrement();
        AddCartItemResponse response = new AddCartItemResponse(
            cartItemId,
            request.getProductId(),
            "상품명",
            request.getOptionId(),
            "색상:블랙",
            request.getQuantity(),
            10000,
            10000 * request.getQuantity(),
            LocalDateTime.now().toString()
        );

        CART_ITEMS.put(cartItemId, new CartItemResponse(
            cartItemId, request.getProductId(), "상품명", request.getOptionId(),
            "색상:블랙", request.getQuantity(), 10000, 10000 * request.getQuantity(), 100
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 장바구니 수량 변경 (PUT /api/cart/{id})
    @Operation(summary = "장바구니 수량 변경", description = "장바구니 아이템의 수량을 변경합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<UpdateCartItemResponse> updateCartItem(
            @PathVariable Long id,
            @RequestBody UpdateCartItemRequest request) {

        CartItemResponse cartItem = CART_ITEMS.get(id);
        if (cartItem == null) {
            throw new RuntimeException("장바구니 아이템을 찾을 수 없습니다.");
        }

        int totalPrice = cartItem.getUnitPrice() * request.getQuantity();
        UpdateCartItemResponse response = new UpdateCartItemResponse(
            id,
            cartItem.getProductId(),
            request.getQuantity(),
            totalPrice,
            LocalDateTime.now().toString()
        );

        CART_ITEMS.put(id, new CartItemResponse(
            id, cartItem.getProductId(), cartItem.getProductName(), cartItem.getOptionId(),
            cartItem.getOptionType(), request.getQuantity(), cartItem.getUnitPrice(), totalPrice, cartItem.getStock()
        ));

        return ResponseEntity.ok(response);
    }

    // 장바구니 삭제 (DELETE /api/cart/{id})
    @Operation(summary = "장바구니 삭제", description = "장바구니에서 특정 아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCartItem(@PathVariable Long id) {
        CART_ITEMS.remove(id);
    }
}