package com.hhplus.ecommerce.presentation.product;

import com.hhplus.ecommerce.application.product.GetPopularProductsUseCase;
import com.hhplus.ecommerce.application.product.GetProductUseCase;
import com.hhplus.ecommerce.application.product.GetProductsUseCase;
import com.hhplus.ecommerce.presentation.product.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "상품 관리", description = "상품 조회 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final GetProductsUseCase getProductsUseCase;
    private final GetProductUseCase getProductUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;

    // 상품 목록 조회 (GET /api/products)
    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ProductListResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<ProductResponse> products = getProductsUseCase.execute();

        return new ProductListResponse(
            products,
            products.size(),
            1,
            size,
            page
        );
    }

    // 상품 상세 조회 (GET /api/products/{id})
    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보와 옵션, 재고를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        ProductDetailResponse productDetail = getProductUseCase.execute(id);
        return ResponseEntity.ok(productDetail);
    }

    // 인기 상품 조회 (GET /api/products/popular)
    @Operation(summary = "인기 상품 조회", description = "조회수와 판매량 기준 인기 상품을 조회합니다.")
    @GetMapping("/popular")
    public PopularProductListResponse getPopularProducts(
            @RequestParam(defaultValue = "5") int limit) {
        List<PopularProductResponse> popularProducts = getPopularProductsUseCase.execute(limit);

        return new PopularProductListResponse(
            popularProducts,
            "조회수 + 판매량 기준",
            LocalDateTime.now().toString()
        );
    }
}