package com.hhplus.ecommerce.domain.cart;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    private CartEntity(Long id, Long userId, Long productId, Long productOptionId, Integer quantity, Long createdAt, Long updatedAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CartEntity create(Long userId, Long productId, Long productOptionId, Integer quantity) {
        long now = System.currentTimeMillis();
        return new CartEntity(null, userId, productId, productOptionId, quantity, now, now);
    }

    public static CartEntity createForTest(Long id, Long userId, Long productId, Long productOptionId, Integer quantity, Long createdAt, Long updatedAt) {
        return new CartEntity(id, userId, productId, productOptionId, quantity, createdAt, updatedAt);
    }

    public void updateQuantity(Integer newQuantity) {
        this.quantity = newQuantity;
        this.updatedAt = System.currentTimeMillis();
    }
}
