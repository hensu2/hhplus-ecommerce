package com.example.demo.controller.product;

import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.PopularProductResponse;
import com.example.demo.dto.response.PopularProductsResponse;
import com.example.demo.dto.response.ProductDetailResponse;
import com.example.demo.dto.response.ProductOptionResponse;
import com.example.demo.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Product", description = "상품 관리 API")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Operation(
            summary = "상품 목록 조회",
            description = "상품 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @Parameter(description = "페이지 번호 (default: 0)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기 (default: 20)")
            @RequestParam(defaultValue = "20") Integer size
    ) {
        // Mock 데이터 생성
        List<ProductResponse> products = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ProductResponse product = new ProductResponse(
                    (long) i,
                    "상품명 " + i,
                    "상품 설명 " + i,
                    10000 * i,
                    LocalDateTime.now()
            );
            products.add(product);
        }

        PageResponse<ProductResponse> response = new PageResponse<>(
                products,
                100L,
                5,
                size,
                page
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "상품 상세 조회",
            description = "특정 상품의 상세 정보와 옵션, 재고를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long id
    ) {
        // Mock 옵션 데이터
        List<ProductOptionResponse> options = new ArrayList<>();
        options.add(new ProductOptionResponse(1L, "색상:블랙", 0, 100));
        options.add(new ProductOptionResponse(2L, "색상:화이트", 1000, 50));

        // Mock 상품 상세 데이터
        ProductDetailResponse response = new ProductDetailResponse(
                id,
                "상품명",
                "상품 설명",
                10000,
                options,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "인기 상품 조회",
            description = "최근 3일간 판매량 기준 Top 5 상품을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인기 상품 조회 성공",
                    content = @Content(schema = @Schema(implementation = PopularProductsResponse.class))
            )
    })
    @GetMapping("/popular")
    public ResponseEntity<PopularProductsResponse> getPopularProducts() {
        // Mock 인기 상품 데이터
        List<PopularProductResponse> products = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            PopularProductResponse product = new PopularProductResponse(
                    (long) i,
                    "인기 상품 " + i,
                    10000 * i,
                    200 - (i * 10),
                    i
            );
            products.add(product);
        }

        PopularProductsResponse response = new PopularProductsResponse(
                products,
                "최근 3일",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // Inner class for error response documentation
    @Schema(description = "에러 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorResponse {
        @Schema(description = "에러 코드", example = "NOT_FOUND")
        private String error;

        @Schema(description = "에러 메시지", example = "상품을 찾을 수 없습니다.")
        private String message;
    }
}