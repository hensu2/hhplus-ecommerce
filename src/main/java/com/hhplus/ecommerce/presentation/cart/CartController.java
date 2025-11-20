package com.hhplus.ecommerce.presentation.cart;

import com.hhplus.ecommerce.application.cart.AddToCartUseCase;
import com.hhplus.ecommerce.application.cart.DeleteCartItemUseCase;
import com.hhplus.ecommerce.application.cart.GetCartUseCase;
import com.hhplus.ecommerce.application.cart.UpdateCartItemUseCase;
import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.cart.CartEntity;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.cart.req.AddToCartRequest;
import com.hhplus.ecommerce.presentation.cart.req.UpdateCartItemRequest;
import com.hhplus.ecommerce.presentation.cart.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "장바구니", description = "장바구니 관리 API")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final DeleteCartItemUseCase deleteCartItemUseCase;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    // 장바구니 조회 (GET /api/cart)
    @Operation(summary = "장바구니 조회", description = "현재 사용자의 장바구니 목록을 조회합니다.")
    @GetMapping
    public CartResponse getCart(@RequestParam Long userId) {
        List<CartEntity> cartEntities = getCartUseCase.execute(userId);

        List<CartItemResponse> items = cartEntities.stream()
                .map(this::toCartItemResponse)
                .toList();

        int totalAmount = items.stream()
                .mapToInt(CartItemResponse::totalPrice)
                .sum();

        return new CartResponse(items, totalAmount);
    }

    // 장바구니 추가 (POST /api/cart)
    @Operation(summary = "장바구니 추가", description = "상품을 장바구니에 추가합니다. 재고를 확인합니다.")
    @PostMapping
    public ResponseEntity<AddCartItemResponse> addToCart(
            @RequestParam Long userId,
            @RequestBody AddToCartRequest request) {
        CartEntity savedCart = addToCartUseCase.execute(
                userId,
                request.productId(),
                request.optionId(),
                request.quantity()
        );

        ProductEntity product = productRepository.getOrThrow(savedCart.getProductId());
        ProductOptionEntity option = productOptionRepository.getOrThrow(savedCart.getProductOptionId());
        int unitPrice = (int) (product.getPrice() + option.getAdditionalPrice());

        AddCartItemResponse response = new AddCartItemResponse(
                savedCart.getId(),
                savedCart.getProductId(),
                product.getProductName(),
                savedCart.getProductOptionId(),
                option.getOptionType(),
                savedCart.getQuantity(),
                unitPrice,
                unitPrice * savedCart.getQuantity(),
                DateTimeUtils.toLocalDateTime(savedCart.getCreatedAt())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 장바구니 수량 변경 (PUT /api/cart/{id})
    @Operation(summary = "장바구니 수량 변경", description = "장바구니 아이템의 수량을 변경합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<UpdateCartItemResponse> updateCartItem(
            @PathVariable Long id,
            @RequestBody UpdateCartItemRequest request) {
        CartEntity savedCart = updateCartItemUseCase.execute(id, request.quantity());

        ProductEntity product = productRepository.getOrThrow(savedCart.getProductId());
        ProductOptionEntity option = productOptionRepository.getOrThrow(savedCart.getProductOptionId());
        int unitPrice = (int) (product.getPrice() + option.getAdditionalPrice());
        int totalPrice = unitPrice * savedCart.getQuantity();

        UpdateCartItemResponse response = new UpdateCartItemResponse(
                savedCart.getId(),
                savedCart.getProductId(),
                savedCart.getQuantity(),
                totalPrice,
                DateTimeUtils.toLocalDateTime(savedCart.getUpdatedAt())
        );
        return ResponseEntity.ok(response);
    }

    // 장바구니 삭제 (DELETE /api/cart/{id})
    @Operation(summary = "장바구니 삭제", description = "장바구니에서 특정 아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCartItem(@PathVariable Long id) {
        deleteCartItemUseCase.execute(id);
    }

    private CartItemResponse toCartItemResponse(CartEntity cart) {
        ProductEntity product = productRepository.getOrThrow(cart.getProductId());
        ProductOptionEntity option = productOptionRepository.getOrThrow(cart.getProductOptionId());
        int unitPrice = (int) (product.getPrice() + option.getAdditionalPrice());
        int totalPrice = unitPrice * cart.getQuantity();
        int stock = option.getStock().intValue();

        return new CartItemResponse(
                cart.getId(),
                cart.getProductId(),
                product.getProductName(),
                cart.getProductOptionId(),
                option.getOptionType(),
                cart.getQuantity(),
                unitPrice,
                totalPrice,
                stock
        );
    }
}