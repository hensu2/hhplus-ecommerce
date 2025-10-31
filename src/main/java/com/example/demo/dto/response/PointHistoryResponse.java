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
public class PointHistoryResponse {
    private Long id;
    private Integer amount;
    private String transactionType;
    private String description;
    private LocalDateTime createdAt;
}
