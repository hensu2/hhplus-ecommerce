package com.hhplus.ecommerce.presentation.cart;

import com.hhplus.ecommerce.application.cart.AddToCartUseCase;
import com.hhplus.ecommerce.application.cart.DeleteCartItemUseCase;
import com.hhplus.ecommerce.application.cart.GetCartUseCase;
import com.hhplus.ecommerce.application.cart.UpdateCartItemUseCase;
import com.hhplus.ecommerce.presentation.cart.req.AddToCartRequest;
import com.hhplus.ecommerce.presentation.cart.req.UpdateCartItemRequest;
import com.hhplus.ecommerce.presentation.cart.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장바구니", description = "장바구니 관리 API")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final AddToCartUseCase addToCartUseCase;
    private final GetCartUseCase getCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final DeleteCartItemUseCase deleteCartItemUseCase;

    // 임시로 사용자 ID를 1L로 고정 (실제로는 인증 정보에서 가져와야 함)
    private static final Long CURRENT_USER_ID = 1L;

    // 장바구니 조회 (GET /api/cart)
    @Operation(summary = "장바구니 조회", description = "현재 사용자의 장바구니 목록을 조회합니다.")
    @GetMapping
    public CartResponse getCart() {
        return getCartUseCase.execute(CURRENT_USER_ID);
    }

    // 장바구니 추가 (POST /api/cart)
    @Operation(summary = "장바구니 추가", description = "상품을 장바구니에 추가합니다. 재고를 확인합니다.")
    @PostMapping
    public ResponseEntity<AddCartItemResponse> addToCart(@RequestBody AddToCartRequest request) {
        AddCartItemResponse response = addToCartUseCase.execute(
            CURRENT_USER_ID,
            request.productId(),
            request.optionId(),
            request.quantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 장바구니 수량 변경 (PUT /api/cart/{id})
    @Operation(summary = "장바구니 수량 변경", description = "장바구니 아이템의 수량을 변경합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<UpdateCartItemResponse> updateCartItem(
            @PathVariable Long id,
            @RequestBody UpdateCartItemRequest request) {
        UpdateCartItemResponse response = updateCartItemUseCase.execute(id, request.quantity());
        return ResponseEntity.ok(response);
    }

    // 장바구니 삭제 (DELETE /api/cart/{id})
    @Operation(summary = "장바구니 삭제", description = "장바구니에서 특정 아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCartItem(@PathVariable Long id) {
        deleteCartItemUseCase.execute(id);
    }
}
