package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAllProductsUseCase {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductEntity> execute() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> executeByKeyword(String keyword) {
        return productRepository.findByProductNameContaining(keyword);
    }
}
