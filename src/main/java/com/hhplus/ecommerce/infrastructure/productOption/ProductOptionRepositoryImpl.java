package com.hhplus.ecommerce.infrastructure.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductOptionRepositoryImpl implements ProductOptionRepository {

    private final ProductOptionTable productOptionTable;

    public ProductOptionRepositoryImpl(ProductOptionTable productOptionTable) {
        this.productOptionTable = productOptionTable;
    }

    @Override
    public List<ProductOptionEntity> findByProductId(Long productId) {
        return productOptionTable.findByProductId(productId);
    }
}