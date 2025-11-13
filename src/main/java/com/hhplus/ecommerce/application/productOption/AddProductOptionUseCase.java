package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.common.exception.ProductNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddProductOptionUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    @Transactional
    public ProductOptionEntity execute(Long productId, String optionType, Long additionalPrice, Long stock) {
        ProductEntity.validateProductId(productId);

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        ProductOptionEntity option = ProductOptionEntity.create(product, optionType, additionalPrice, stock);
        return productOptionRepository.save(option);
    }
}