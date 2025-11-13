package com.hhplus.ecommerce.domain.product;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT", indexes = {
    @Index(name = "idx_created_user_id", columnList = "created_user_id"),
    @Index(name = "idx_product_name", columnList = "product_name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_user"))
    private UserEntity createdUser;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "price", nullable = false)
    private Long price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ProductEntity(Long id, UserEntity createdUser, String productName, String content, Long price) {
        this.id = id;
        this.createdUser = createdUser;
        this.productName = productName;
        this.content = content;
        this.price = price;
    }

    public static void validateProductId(Long productId) {
        if (productId == null) {
            throw new InvalidInputException("Product ID cannot be null");
        }
        if (productId <= 0) {
            throw new InvalidInputException("Product ID must be greater than 0");
        }
    }

    /**
     * 상품 생성
     */
    public static ProductEntity create(UserEntity createdUser, String productName, String content, Long price) {
        validateCreatedUser(createdUser);
        validateProductName(productName);
        validatePrice(price);
        return new ProductEntity(null, createdUser, productName, content, price);
    }

    /**
     * 테스트용 팩토리 메서드 - ID 포함
     */
    public static ProductEntity createForTest(Long id, UserEntity createdUser, String productName, String content, Long price) {
        validateCreatedUser(createdUser);
        validateProductName(productName);
        validatePrice(price);
        return new ProductEntity(id, createdUser, productName, content, price);
    }

    /**
     * 상품 정보 수정
     */
    public void update(String productName, String content, Long price) {
        if (productName != null && !productName.trim().isEmpty()) {
            validateProductName(productName);
            this.productName = productName;
        }
        if (content != null) {
            this.content = content;
        }
        if (price != null) {
            validatePrice(price);
            this.price = price;
        }
    }

    private static void validateCreatedUser(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("상품 등록자 정보는 필수입니다.");
        }
    }

    private static void validateProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (productName.length() > 255) {
            throw new IllegalArgumentException("상품명은 255자를 초과할 수 없습니다.");
        }
    }

    private static void validatePrice(Long price) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("상품 가격은 0 이상이어야 합니다.");
        }
    }
}