package com.hhplus.ecommerce.domain.productOption;

import com.hhplus.ecommerce.common.exception.InvalidStockUpdateException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_OPTIONS", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_option_product"))
    private ProductEntity product;

    @Column(name = "option_type", nullable = false, length = 100)
    private String optionType;

    @Column(name = "additional_price", nullable = false)
    private Long additionalPrice = 0L;

    @Column(name = "stock", nullable = false)
    private Long stock = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    private ProductOptionEntity(Long id, ProductEntity product, String optionType, Long additionalPrice, Long stock) {
        this.id = id;
        this.product = product;
        this.optionType = optionType;
        this.additionalPrice = additionalPrice;
        this.stock = stock;
    }

    /**
     * 상품 옵션 생성
     */
    public static ProductOptionEntity create(ProductEntity product, String optionType, Long additionalPrice, Long stock) {
        validateProduct(product);
        validateOptionType(optionType);
        validateAdditionalPrice(additionalPrice);
        validateStock(stock);
        return new ProductOptionEntity(null, product, optionType, additionalPrice, stock);
    }

    /**
     * 테스트용 팩토리 메서드 - ID 포함
     */
    public static ProductOptionEntity createForTest(Long id, ProductEntity product, String optionType, Long additionalPrice, Long stock) {
        validateProduct(product);
        validateOptionType(optionType);
        validateAdditionalPrice(additionalPrice);
        validateStock(stock);
        return new ProductOptionEntity(id, product, optionType, additionalPrice, stock);
    }

    /**
     * 재고 업데이트
     */
    public void updateStock(StockUpdateType type, Long amount) {
        Long newStock;
        if (type == StockUpdateType.SET) {
            newStock = amount;
        } else if (type == StockUpdateType.INCREASE) {
            newStock = this.stock + amount;
        } else if (type == StockUpdateType.DECREASE) {
            newStock = this.stock - amount;
            if (newStock < 0) {
                throw new InvalidStockUpdateException("재고가 부족합니다. 현재 재고: " + this.stock);
            }
        } else {
            throw new IllegalArgumentException("Invalid stock update type: " + type);
        }
        validateStock(newStock);
        this.stock = newStock;
    }

    /**
     * 옵션 정보 수정
     */
    public void update(String optionType, Long additionalPrice) {
        if (optionType != null && !optionType.trim().isEmpty()) {
            validateOptionType(optionType);
            this.optionType = optionType;
        }
        if (additionalPrice != null) {
            validateAdditionalPrice(additionalPrice);
            this.additionalPrice = additionalPrice;
        }
    }

    private static void validateProduct(ProductEntity product) {
        if (product == null) {
            throw new IllegalArgumentException("상품 정보는 필수입니다.");
        }
    }

    private static void validateOptionType(String optionType) {
        if (optionType == null || optionType.trim().isEmpty()) {
            throw new IllegalArgumentException("옵션 타입은 필수입니다.");
        }
        if (optionType.length() > 100) {
            throw new IllegalArgumentException("옵션 타입은 100자를 초과할 수 없습니다.");
        }
    }

    private static void validateAdditionalPrice(Long additionalPrice) {
        if (additionalPrice == null || additionalPrice < 0) {
            throw new IllegalArgumentException("추가 가격은 0 이상이어야 합니다.");
        }
    }

    private static void validateStock(Long stock) {
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
    }
}
