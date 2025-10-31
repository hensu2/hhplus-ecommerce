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
public class ChargePointResponse {
    private Long userId;
    private Integer amount;
    private Integer afterBalance;
    private String transactionType;
    private LocalDateTime chargedAt;
}
