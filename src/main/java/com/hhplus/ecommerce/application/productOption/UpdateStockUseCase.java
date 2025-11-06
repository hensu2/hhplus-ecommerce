package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.common.exception.ProductOptionNotFoundException;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
import com.hhplus.ecommerce.presentation.productOption.res.UpdateStockResponse;
import org.springframework.stereotype.Service;

@Service
public class UpdateStockUseCase {

    private final ProductOptionRepository productOptionRepository;

    public UpdateStockUseCase(ProductOptionRepository productOptionRepository) {
        this.productOptionRepository = productOptionRepository;
    }

    public UpdateStockResponse execute(Long optionId, UpdateStockRequest request) {
        ProductOptionEntity option = productOptionRepository.findById(optionId)
            .orElseThrow(() -> new ProductOptionNotFoundException("옵션을 찾을 수 없습니다. ID: " + optionId));

        ProductOptionEntity updatedOption = option.updateStock(request.getType(), request.getAmount());
        productOptionRepository.save(updatedOption);

        return updatedOption.toUpdateStockResponse();
    }
}