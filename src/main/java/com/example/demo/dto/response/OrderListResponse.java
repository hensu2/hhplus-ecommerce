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
public class OrderListResponse {
    private Long orderId;
    private String status;
    private Integer totalAmount;
    private Integer finalAmount;
    private Integer itemCount;
    private LocalDateTime orderedAt;
}
