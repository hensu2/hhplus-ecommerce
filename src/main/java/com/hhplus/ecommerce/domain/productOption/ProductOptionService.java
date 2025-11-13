package com.hhplus.ecommerce.domain.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductOptionService {

    private final ProductOptionRepository productOptionRepository;

    public ProductOptionEntity decreaseStock(Long optionId, int quantity) {
        ProductOptionEntity option = productOptionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

        option.updateStock(StockUpdateType.DECREASE, (long) quantity);
        return productOptionRepository.save(option);
    }
}
