package com.hhplus.ecommerce.domain.productOption;

import java.util.List;

public interface ProductOptionRepository {
    List<ProductOptionEntity> findByProductId(Long productId);
}