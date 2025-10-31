package com.example.demo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "로그인 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @Schema(description = "사용자명", example = "user123", required = true)
    private String username;

    @Schema(description = "비밀번호", example = "password123", required = true)
    private String password;
}
