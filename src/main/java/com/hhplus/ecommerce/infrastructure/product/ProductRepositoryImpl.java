package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductTable productTable;

    public ProductRepositoryImpl(ProductTable productTable) {
        this.productTable = productTable;
    }

    @Override
    public List<Product> findAll() {
        return productTable.findAll();
    }
}
