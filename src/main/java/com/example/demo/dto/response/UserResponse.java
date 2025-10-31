package com.example.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "사용자 정보 응답")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자명", example = "user123")
    private String username;

    @Schema(description = "보유 포인트", example = "50000")
    private Integer point;

    @Schema(description = "권한", example = "USER")
    private String role;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    public UserResponse(long l, String user123, int i, String user, LocalDateTime now, LocalDateTime now1) {
    }
}
