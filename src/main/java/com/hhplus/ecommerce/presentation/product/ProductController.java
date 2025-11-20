package com.hhplus.ecommerce.presentation.product;

import com.hhplus.ecommerce.application.product.GetPopularProductsUseCase;
import com.hhplus.ecommerce.application.product.GetProductUseCase;
import com.hhplus.ecommerce.application.product.GetProductsUseCase;
import com.hhplus.ecommerce.application.productOption.GetProductOptionsUseCase;
import com.hhplus.ecommerce.common.util.DateTimeUtils;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.presentation.product.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "상품 관리", description = "상품 조회 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final GetProductsUseCase getProductsUseCase;
    private final GetProductUseCase getProductUseCase;
    private final GetProductOptionsUseCase getProductOptionsUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;
    private final ProductRepository productRepository;

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ProductListResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<ProductEntity> productPage = getProductsUseCase.execute(pageable);

        List<ProductResponse> products = productPage.getContent().stream()
                .map(ProductResponse::new)
                .toList();

        return new ProductListResponse(
            products,
            (int) productPage.getTotalElements(),
            productPage.getTotalPages(),
            productPage.getSize(),
            productPage.getNumber()
        );
    }

    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보와 옵션, 재고를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        ProductEntity product = getProductUseCase.execute(id);
        List<ProductOptionEntity> options = getProductOptionsUseCase.execute(id);
        return ResponseEntity.ok(new ProductDetailResponse(product, options));
    }

    @Operation(summary = "인기 상품 조회", description = "조회수와 판매량 기준 인기 상품을 조회합니다.")
    @GetMapping("/popular")
    public PopularProductListResponse getPopularProducts(
            @RequestParam(defaultValue = "5") int limit) {
        List<ProductStatisticsEntity> statistics = getPopularProductsUseCase.execute(limit);

        List<PopularProductResponse> popularProducts = statistics.stream()
                .map(stats -> {
                    ProductEntity product = productRepository.getOrThrow(stats.getProductId());
                    return new PopularProductResponse(product, stats);
                })
                .toList();

        return new PopularProductListResponse(
            popularProducts,
            "조회수 + 판매량 기준",
            DateTimeUtils.now()
        );
    }
}