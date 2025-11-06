package com.hhplus.ecommerce.presentation.productOption;

import com.hhplus.ecommerce.application.productOption.GetProductStockUseCase;
import com.hhplus.ecommerce.presentation.productOption.res.ProductStockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상품 옵션 관리", description = "상품 옵션 및 재고 조회 API")
@RestController
@RequestMapping("/api/product-options")
public class ProductOptionController {

    private final GetProductStockUseCase getProductStockUseCase;

    public ProductOptionController(GetProductStockUseCase getProductStockUseCase) {
        this.getProductStockUseCase = getProductStockUseCase;
    }

    // 상품별 옵션 재고 조회 (GET /api/product-options/stock/{productId})
    @Operation(summary = "상품별 옵션 재고 조회", description = "특정 상품의 모든 옵션별 재고 현황을 조회합니다.")
    @GetMapping("/stock/{productId}")
    public ResponseEntity<ProductStockResponse> getProductStock(@PathVariable Long productId) {
        ProductStockResponse stockResponse = getProductStockUseCase.execute(productId);
        return ResponseEntity.ok(stockResponse);
    }
}