package com.hhplus.ecommerce.presentation.product;

import com.hhplus.ecommerce.application.product.ProductFacade;
import com.hhplus.ecommerce.presentation.product.req.CreateProductRequest;
import com.hhplus.ecommerce.presentation.product.res.ProductDetailResponse;
import com.hhplus.ecommerce.presentation.productOption.res.ProductOptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Product", description = "상품 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductFacade productFacade;

    @Operation(summary = "전체 상품 조회", description = "모든 상품을 조회합니다")
    @GetMapping
    public List<ProductDetailResponse> getProducts(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productFacade.searchProducts(keyword);
        } else {
            return productFacade.getAllProducts();
        }
    }

    @Operation(summary = "단일 상품 조회", description = "상품 ID로 상품을 조회합니다")
    @GetMapping("/{productId}")
    public ProductDetailResponse getProduct(@PathVariable Long productId) {
        return productFacade.getProduct(productId);
    }

    @Operation(summary = "상품 옵션 조회", description = "특정 상품의 모든 옵션 목록을 조회합니다")
    @GetMapping("/{productId}/options")
    public List<ProductOptionResponse> getProductOptions(@PathVariable Long productId) {
        return productFacade.getProductOptions(productId);
    }

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다")
    @PostMapping
    public ProductDetailResponse createProduct(@RequestBody CreateProductRequest request) {
        return productFacade.registerProduct(
                request.createdUserId(),
                request.productName(),
                request.content(),
                request.price()
        );
    }
}
