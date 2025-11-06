package com.hhplus.ecommerce.domain.product;

import com.hhplus.ecommerce.infrastructure.product.Product;

import java.util.List;

public interface ProductRepository {
    List<Product> findAll();
}
