package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
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

    @Transactional(readOnly = true)
    public Page<ProductEntity> execute(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
}
