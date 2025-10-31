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
public class CancelOrderResponse {
    private Long orderId;
    private String status;
    private Integer refundAmount;
    private Integer refundPoint;
    private Boolean couponRestored;
    private LocalDateTime cancelledAt;
}
