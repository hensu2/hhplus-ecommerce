package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long optionId;
    private String optionType;
    private Integer quantity;
    private Integer unitPrice;
    private Integer totalPrice;
    private Integer stock;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;

    public CartItemResponse(Long id, Long productId, String productName, Long optionId,
                           String optionType, Integer quantity, Integer unitPrice,
                           Integer totalPrice, Integer stock) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.optionId = optionId;
        this.optionType = optionType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.stock = stock;
    }
}
