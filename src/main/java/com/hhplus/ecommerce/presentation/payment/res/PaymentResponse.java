package com.hhplus.ecommerce.presentation.payment.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Integer paymentAmount;
    private String paymentMethod;
    private String status;
    private String paidAt;
    private Integer earnedPoint;
}
