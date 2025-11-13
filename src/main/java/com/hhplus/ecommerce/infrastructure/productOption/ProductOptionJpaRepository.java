package com.hhplus.ecommerce.infrastructure.productOption;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOptionEntity, Long> {

    /**
     * 상품 ID로 옵션 조회
     */
    @Query("SELECT po FROM ProductOptionEntity po WHERE po.product.id = :productId")
    List<ProductOptionEntity> findByProductId(@Param("productId") Long productId);

    /**
     * 재고가 있는 옵션만 조회
     */
    @Query("SELECT po FROM ProductOptionEntity po WHERE po.product.id = :productId AND po.stock > 0")
    List<ProductOptionEntity> findAvailableOptionsByProductId(@Param("productId") Long productId);
}
