package com.hhplus.ecommerce.presentation.productOption;

import com.hhplus.ecommerce.application.productOption.AddProductOptionUseCase;
import com.hhplus.ecommerce.application.productOption.GetProductOptionsUseCase;
import com.hhplus.ecommerce.application.productOption.GetProductStockUseCase;
import com.hhplus.ecommerce.application.productOption.UpdateStockUseCase;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.presentation.productOption.req.AddProductOptionRequest;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
import com.hhplus.ecommerce.presentation.productOption.res.ProductOptionResponse;
import com.hhplus.ecommerce.presentation.productOption.res.ProductStockResponse;
import com.hhplus.ecommerce.presentation.productOption.res.UpdateStockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "상품 옵션 관리", description = "상품 옵션 및 재고 조회 API")
@RestController
@RequestMapping("/api/product-options")
@RequiredArgsConstructor
public class ProductOptionController {

    private final GetProductStockUseCase getProductStockUseCase;
    private final UpdateStockUseCase updateStockUseCase;
    private final AddProductOptionUseCase addProductOptionUseCase;
    private final GetProductOptionsUseCase getProductOptionsUseCase;

    // 상품별 옵션 재고 조회 (GET /api/product-options/stock/{productId})
    @Operation(summary = "상품별 옵션 재고 조회", description = "특정 상품의 모든 옵션별 재고 현황을 조회합니다.")
    @GetMapping("/stock/{productId}")
    public ResponseEntity<ProductStockResponse> getProductStock(@PathVariable Long productId) {
        ProductStockResponse stockResponse = getProductStockUseCase.execute(productId);
        return ResponseEntity.ok(stockResponse);
    }

    // 상품 옵션 재고 업데이트 (PATCH /api/product-options/{optionId}/stock)
    @Operation(summary = "상품 옵션 재고 업데이트", description = "특정 옵션의 재고를 업데이트합니다. (SET: 설정, INCREASE: 증가, DECREASE: 감소)")
    @PatchMapping("/{optionId}/stock")
    public ResponseEntity<UpdateStockResponse> updateStock(
        @PathVariable Long optionId,
        @RequestBody @Valid UpdateStockRequest request
    ) {
        UpdateStockResponse response = updateStockUseCase.execute(optionId, request);
        return ResponseEntity.ok(response);
    }

    // 상품 옵션 추가 (POST /api/product-options)
    @Operation(summary = "상품 옵션 추가", description = "특정 상품에 새로운 옵션을 추가합니다.")
    @PostMapping
    public ResponseEntity<ProductOptionResponse> addProductOption(@RequestBody @Valid AddProductOptionRequest request) {
        ProductOptionEntity option = addProductOptionUseCase.execute(
            request.productId(),
            request.optionType(),
            request.additionalPrice(),
            request.stock()
        );
        return ResponseEntity.ok(ProductOptionResponse.from(option));
    }

    // 상품별 옵션 목록 조회 (GET /api/product-options/product/{productId})
    @Operation(summary = "상품별 옵션 목록 조회", description = "특정 상품의 모든 옵션 목록을 조회합니다.")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductOptionResponse>> getProductOptions(@PathVariable Long productId) {
        List<ProductOptionEntity> options = getProductOptionsUseCase.execute(productId);
        List<ProductOptionResponse> responses = options.stream()
            .map(ProductOptionResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}