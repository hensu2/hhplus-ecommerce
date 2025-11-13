package com.hhplus.ecommerce.domain.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "option_type", nullable = false)
    private String optionType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    private OrderItemEntity(Long id, Long orderId, Long productId, Long productOptionId, String productName,
                            String optionType, Integer quantity, Integer price, Long createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.optionType = optionType;
        this.quantity = quantity;
        this.price = price;
        this.createdAt = createdAt;
    }

    public static OrderItemEntity create(Long orderId, Long productId, Long productOptionId, String productName,
                                         String optionType, Integer quantity, Integer price) {
        return new OrderItemEntity(null, orderId, productId, productOptionId, productName, optionType, quantity, price, System.currentTimeMillis());
    }

    public static OrderItemEntity createForTest(Long id, Long orderId, Long productId, Long productOptionId,
                                                String productName, String optionType, Integer quantity, Integer price, Long createdAt) {
        return new OrderItemEntity(id, orderId, productId, productOptionId, productName, optionType, quantity, price, createdAt);
    }
}
