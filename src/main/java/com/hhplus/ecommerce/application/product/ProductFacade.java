package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.application.productOption.GetProductOptionsUseCase;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.presentation.product.res.ProductDetailResponse;
import com.hhplus.ecommerce.presentation.productOption.res.ProductOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductFacade
 * - Controller와 UseCase 사이의 파사드 레이어
 * - 여러 상품 관련 UseCase를 조율
 * - Controller의 복잡도를 낮추고 각 레이어의 책임을 명확히 분리
 */
@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final GetAllProductsUseCase getAllProductsUseCase;
    private final GetProductUseCase getProductUseCase;
    private final RegisterProductUseCase registerProductUseCase;
    private final GetProductOptionsUseCase getProductOptionsUseCase;

    /**
     * 전체 상품 조회
     */
    public List<ProductDetailResponse> getAllProducts() {
        List<ProductEntity> products = getAllProductsUseCase.execute();
        return products.stream()
                .map(ProductDetailResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 키워드로 상품 검색
     */
    public List<ProductDetailResponse> searchProducts(String keyword) {
        List<ProductEntity> products = getAllProductsUseCase.executeByKeyword(keyword);
        return products.stream()
                .map(ProductDetailResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 상품 조회
     */
    public ProductDetailResponse getProduct(Long productId) {
        return getProductUseCase.execute(productId);
    }

    /**
     * 상품 옵션 목록 조회
     */
    public List<ProductOptionResponse> getProductOptions(Long productId) {
        List<ProductOptionEntity> options = getProductOptionsUseCase.execute(productId);
        return options.stream()
                .map(ProductOptionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상품 등록
     */
    public ProductDetailResponse registerProduct(Long createdUserId, String productName, String content, Long price) {
        ProductEntity product = registerProductUseCase.execute(createdUserId, productName, content, price);
        return ProductDetailResponse.from(product);
    }
}
