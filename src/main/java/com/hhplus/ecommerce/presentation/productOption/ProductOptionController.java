package com.hhplus.ecommerce.presentation.productOption;

import com.hhplus.ecommerce.application.productOption.GetProductStockUseCase;
import com.hhplus.ecommerce.application.productOption.UpdateStockUseCase;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
import com.hhplus.ecommerce.presentation.productOption.res.ProductStockResponse;
import com.hhplus.ecommerce.presentation.productOption.res.UpdateStockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상품 옵션 관리", description = "상품 옵션 및 재고 조회 API")
@RestController
@RequestMapping("/api/product-options")
public class ProductOptionController {

    private final GetProductStockUseCase getProductStockUseCase;
    private final UpdateStockUseCase updateStockUseCase;

    public ProductOptionController(GetProductStockUseCase getProductStockUseCase, UpdateStockUseCase updateStockUseCase) {
        this.getProductStockUseCase = getProductStockUseCase;
        this.updateStockUseCase = updateStockUseCase;
    }

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
}