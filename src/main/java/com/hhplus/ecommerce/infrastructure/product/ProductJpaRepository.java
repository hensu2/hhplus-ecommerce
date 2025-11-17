package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 상품명으로 검색
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.productName LIKE %:keyword%")
    List<ProductEntity> findByProductNameContaining(@Param("keyword") String keyword);

    /**
     * 생성자로 상품 조회
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.createdUser.id = :userId")
    List<ProductEntity> findByCreatedUserId(@Param("userId") Long userId);
}