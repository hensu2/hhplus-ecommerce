package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Integer paymentAmount;
    private String paymentMethod;
    private String status;
    private LocalDateTime paidAt;
    private Integer earnedPoint;
}
