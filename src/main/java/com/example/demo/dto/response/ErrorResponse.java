package com.example.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "에러 응답")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @Schema(description = "에러 코드", example = "UNAUTHORIZED")
    private String error;

    @Schema(description = "에러 메시지", example = "인증이 필요합니다.")
    private String message;
}
