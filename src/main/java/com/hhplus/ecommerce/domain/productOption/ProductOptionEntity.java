package com.hhplus.ecommerce.domain.productOption;

import com.hhplus.ecommerce.common.exception.InvalidStockUpdateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "product_options")
public class ProductOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "option_type", nullable = false)
    private String optionType;

    @Column(name = "additional_price", nullable = false)
    private Long additionalPrice;

    @Column(nullable = false)
    private Long stock;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public ProductOptionEntity updateStock(StockUpdateType type, Integer amount) {
        long newStock = switch (type) {
            case SET -> amount;
            case INCREASE -> this.stock + amount;
            case DECREASE -> {
                long result = this.stock - amount;
                if (result < 0) {
                    throw new InvalidStockUpdateException("재고가 부족합니다. 현재 재고: " + this.stock);
                }
                yield result;
            }
        };

        this.stock = newStock;
        return this;
    }
}