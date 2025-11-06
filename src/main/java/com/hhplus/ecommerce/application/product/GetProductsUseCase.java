package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.product.Product;
import com.hhplus.ecommerce.presentation.product.res.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetProductsUseCase {

    private final ProductRepository productRepository;

    public GetProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> execute() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(Product::toProductResponse)
                .collect(Collectors.toList());
    }
}
