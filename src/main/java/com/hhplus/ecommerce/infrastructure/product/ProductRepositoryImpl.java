package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.infrastructure.product.memory.ProductTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductTable productTable;

    @Override
    public List<ProductEntity> findAll() {
        return productTable.findAll();
    }

    @Override
    public Optional<ProductEntity> findById(Long id) {
        return productTable.findById(id);
    }
}
