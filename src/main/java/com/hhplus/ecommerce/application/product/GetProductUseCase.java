package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.common.exception.ProductNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.presentation.product.res.ProductDetailResponse;
import com.hhplus.ecommerce.presentation.product.res.ProductOptionResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetProductUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    public GetProductUseCase(ProductRepository productRepository, ProductOptionRepository productOptionRepository) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
    }

    public ProductDetailResponse execute(Long productId) {
        ProductEntity.validateProductId(productId);

        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        List<ProductOptionEntity> options = productOptionRepository.findByProductId(productId);

        List<ProductOptionResponse> optionResponses = options.stream()
            .map(option -> new ProductOptionResponse(
                option.id(),
                option.optionType(),
                (int) option.additionalPrice(),
                (int) option.stock()
            ))
            .collect(Collectors.toList());

        return new ProductDetailResponse(
            product.id(),
            product.productName(),
            product.content(),
            (int) product.price(),
            optionResponses,
            product.getFormattedCreatedAt()
        );
    }
}
