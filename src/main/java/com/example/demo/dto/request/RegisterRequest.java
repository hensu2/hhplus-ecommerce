package com.example.demo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "회원가입 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @Schema(description = "사용자명", example = "newuser", required = true)
    private String username;

    @Schema(description = "비밀번호", example = "password123", required = true)
    private String password;

    @Schema(description = "비밀번호 확인", example = "password123", required = true)
    private String passwordConfirm;
}
