package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.common.exception.ProductNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.productOption.res.ProductStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProductStockUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    public ProductStockResponse execute(Long productId) {
        ProductEntity.validateProductId(productId);

        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        List<ProductOptionEntity> options = productOptionRepository.findByProductId(productId);

        return product.toProductStockResponse(options);
    }
}