package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProductOptionsUseCase {

    private final ProductOptionRepository productOptionRepository;

    @Transactional(readOnly = true)
    public List<ProductOptionEntity> execute(Long productId) {
        return productOptionRepository.findByProductId(productId);
    }
}