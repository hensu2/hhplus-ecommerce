package com.hhplus.ecommerce.presentation.user.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private Long point;
    private String role;
    private Long createdAt;
    private Long updatedAt;
}