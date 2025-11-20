package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.common.exception.ProductOptionNotFoundException;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateStockUseCase {

    private final ProductOptionRepository productOptionRepository;

    public ProductOptionEntity execute(Long optionId, UpdateStockRequest request) {
        ProductOptionEntity option = productOptionRepository.findById(optionId)
            .orElseThrow(() -> new ProductOptionNotFoundException("옵션을 찾을 수 없습니다. ID: " + optionId));

        option.updateStock(request.type(), request.amount());
        return productOptionRepository.save(option);
    }
}