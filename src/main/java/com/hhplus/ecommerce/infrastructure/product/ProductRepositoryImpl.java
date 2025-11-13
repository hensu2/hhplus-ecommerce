package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public ProductEntity save(ProductEntity product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<ProductEntity> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<ProductEntity> findAll() {
        return productJpaRepository.findAll();
    }

    @Override
    public List<ProductEntity> findByProductNameContaining(String keyword) {
        return productJpaRepository.findByProductNameContaining(keyword);
    }

    @Override
    public List<ProductEntity> findByCreatedUserId(Long userId) {
        return productJpaRepository.findByCreatedUserId(userId);
    }
}
