package com.hhplus.ecommerce.infrastructure.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.productOption.memory.ProductOptionTable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<ProductOptionEntity> findById(Long id) {
        return Optional.ofNullable(productOptionTable.findById(id));
    }

    @Override
    public ProductOptionEntity save(ProductOptionEntity productOption) {
        return productOptionTable.save(productOption);
    }
}