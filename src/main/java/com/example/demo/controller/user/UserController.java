package com.example.demo.controller.user;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.response.ErrorResponse;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Operation(
            summary = "사용자 정보 조회",
            description = "현재 로그인한 사용자의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @Parameter(description = "인증 토큰", required = true)
            @RequestHeader("Authorization") String authorization
    ) {
        // Mock: Authorization 헤더에서 사용자 정보 추출 (실제로는 JWT 파싱 등)
        UserResponse response = new UserResponse(
                1L,
                "user123",
                50000,
                "USER",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "사용자 로그인",
            description = "사용자 로그인을 처리하고 인증 토큰을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그인 요청",
                    required = true
            )
            @RequestBody LoginRequest request
    ) {
        // Mock 로그인 처리
        LoginResponse response = new LoginResponse(
                "mock-jwt-token-" + request.getUsername(),
                "Bearer",
                3600L,
                1L,
                request.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "사용자 회원가입",
            description = "새로운 사용자를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 중복된 사용자명",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원가입 요청",
                    required = true
            )
            @RequestBody RegisterRequest request
    ) {
        // Mock 회원가입 처리
        UserResponse response = new UserResponse(
                1L,
                request.getUsername(),
                0,  // 초기 포인트 0
                "USER",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}