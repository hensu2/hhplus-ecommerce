package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.presentation.product.res.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetProductsUseCase {

    private final ProductRepository productRepository;

    public List<ProductResponse> execute() {
        List<ProductEntity> products = productRepository.findAll();
        return products.stream()
                .map(ProductEntity::toProductResponse)
                .collect(Collectors.toList());
    }
}
