package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.infrastructure.cache.ProductCacheService;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductsUseCase {

    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;

    @Transactional(readOnly = true)
    public Page<ProductEntity> execute(Pageable pageable) {
        // 1. 캐시 조회
        return productCacheService.getProductListCache(pageable)
            .orElseGet(() -> {
                // 2. 캐시 미스 - DB 조회
                Page<ProductEntity> products = productRepository.findAll(pageable);

                // 3. 캐시 저장
                productCacheService.setProductListCache(pageable, products);

                return products;
            });
    }
}
