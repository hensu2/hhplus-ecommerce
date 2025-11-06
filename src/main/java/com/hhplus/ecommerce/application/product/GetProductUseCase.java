package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.common.exception.ProductNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import com.hhplus.ecommerce.infrastructure.product.memory.ProductStatisticsTable;
import com.hhplus.ecommerce.presentation.product.res.ProductDetailResponse;
import com.hhplus.ecommerce.presentation.product.res.ProductOptionResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetProductUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductStatisticsRepository productStatisticsRepository;
    private final ProductStatisticsTable productStatisticsTable;

    public GetProductUseCase(ProductRepository productRepository,
                            ProductOptionRepository productOptionRepository,
                            ProductStatisticsRepository productStatisticsRepository,
                            ProductStatisticsTable productStatisticsTable) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.productStatisticsRepository = productStatisticsRepository;
        this.productStatisticsTable = productStatisticsTable;
    }

    public ProductDetailResponse execute(Long productId) {
        ProductEntity.validateProductId(productId);

        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        // 조회수 증가 (원자적 연산)
        ProductStatisticsEntity updated = productStatisticsTable.incrementViewCount(productId);
        productStatisticsRepository.save(updated);

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
