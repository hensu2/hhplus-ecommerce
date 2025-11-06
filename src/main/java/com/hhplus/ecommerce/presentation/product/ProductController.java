package com.hhplus.ecommerce.presentation.product;

import com.hhplus.ecommerce.application.product.GetProductsUseCase;
import com.hhplus.ecommerce.presentation.product.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "상품 관리", description = "상품 조회 API")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final GetProductsUseCase getProductsUseCase;

    public ProductController(GetProductsUseCase getProductsUseCase) {
        this.getProductsUseCase = getProductsUseCase;
    }

    private static final Map<Long, List<ProductOptionResponse>> PRODUCT_OPTIONS = Map.of(
        1L, List.of(
            new ProductOptionResponse(1L, "색상:블랙", 0, 100),
            new ProductOptionResponse(2L, "색상:실버", 10000, 50)
        ),
        2L, List.of(
            new ProductOptionResponse(3L, "축:청축", 0, 80),
            new ProductOptionResponse(4L, "축:적축", 0, 70)
        )
    );

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
        List<ProductResponse> products = getProductsUseCase.execute();
        ProductResponse product = products.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        List<ProductOptionResponse> options = PRODUCT_OPTIONS.getOrDefault(id, List.of());

        return ResponseEntity.ok(new ProductDetailResponse(
            product.getId(),
            product.getProductName(),
            product.getContent(),
            product.getPrice(),
            options,
            product.getCreatedAt()
        ));
    }

    // 인기 상품 조회 (GET /api/products/popular)
    @Operation(summary = "인기 상품 조회", description = "최근 3일간 판매량 기준 Top 5 상품을 조회합니다.")
    @GetMapping("/popular")
    public PopularProductListResponse getPopularProducts() {
        List<PopularProductResponse> popularProducts = List.of(
            new PopularProductResponse(1L, "노트북", 890000, 150, 1),
            new PopularProductResponse(2L, "키보드", 120000, 120, 2),
            new PopularProductResponse(3L, "마우스", 85000, 100, 3),
            new PopularProductResponse(4L, "모니터", 350000, 85, 4),
            new PopularProductResponse(5L, "헤드셋", 150000, 70, 5)
        );

        return new PopularProductListResponse(
            popularProducts,
            "최근 3일",
            LocalDateTime.now().toString()
        );
    }
}