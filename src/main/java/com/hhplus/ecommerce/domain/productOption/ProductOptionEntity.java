package com.hhplus.ecommerce.domain.productOption;

public record ProductOptionEntity(
    long id,
    long productId,
    String optionType,
    long additionalPrice,
    long stock,
    long createdAt,
    long updatedAt
) {}
