package com.hhplus.ecommerce.presentation.user.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointResponse {
    private Long userId;
    private String username;
    private Integer point;
    private String updatedAt;
}
