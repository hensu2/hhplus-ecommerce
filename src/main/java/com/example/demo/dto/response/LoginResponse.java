package com.example.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "로그인 응답")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @Schema(description = "인증 토큰", example = "mock-jwt-token-user123")
    private String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "만료 시간 (초)", example = "3600")
    private Long expiresIn;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자명", example = "user123")
    private String username;
}
