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
public class CouponResponse {
    private Long id;
    private Long couponId;
    private String couponName;
    private String discountType;
    private Integer discountAmount;
    private Integer useMinAmount;
    private Integer useMaxAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
}
