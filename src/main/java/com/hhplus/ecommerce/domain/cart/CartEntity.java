package com.hhplus.ecommerce.domain.cart;

public record CartEntity(
    Long id,
    Long userId,
    Long productId,
    Long productOptionId,
    Integer quantity,
    Long createdAt,
    Long updatedAt
) {
    public static CartEntity create(Long userId, Long productId, Long productOptionId, Integer quantity) {
        long now = System.currentTimeMillis();
        return new CartEntity(0L, userId, productId, productOptionId, quantity, now, now);
    }

    public CartEntity updateQuantity(Integer newQuantity) {
        return new CartEntity(id, userId, productId, productOptionId, newQuantity, createdAt, System.currentTimeMillis());
    }

    public CartEntity withId(Long newId) {
        return new CartEntity(newId, userId, productId, productOptionId, quantity, createdAt, updatedAt);
    }
}
