package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.common.exception.UserNotFoundException;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserUseCase 단위 테스트")
class GetUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserUseCase getUserUseCase;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testUser = new UserEntity(1L, "testuser", 50000L, "USER", timestamp, timestamp);
    }

    @Test
    @DisplayName("유효한 사용자 ID로 사용자를 조회한다")
    void execute_ValidUserId_ReturnsUserEntity() {
        // given
        Long userId = 1L;
        given(userRepository.getOrThrow(userId)).willReturn(testUser);

        // when
        UserEntity result = getUserUseCase.execute(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(result.getPoint()).isEqualTo(testUser.getPoint());
        assertThat(result.getRole()).isEqualTo(testUser.getRole());
        verify(userRepository).getOrThrow(userId);
    }

    @Test
    @DisplayName("null 사용자 ID는 예외를 발생시킨다")
    void execute_NullUserId_ThrowsException() {
        // given
        Long nullId = null;

        // when & then
        assertThatThrownBy(() -> getUserUseCase.execute(nullId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("0 이하의 사용자 ID는 예외를 발생시킨다")
    void execute_InvalidUserId_ThrowsException() {
        // given
        Long invalidId = 0L;

        // when & then
        assertThatThrownBy(() -> getUserUseCase.execute(invalidId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("User ID must be greater than 0");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID는 예외를 발생시킨다")
    void execute_UserNotFound_ThrowsException() {
        // given
        Long userId = 999L;
        given(userRepository.getOrThrow(userId)).willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> getUserUseCase.execute(userId))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("사용자를 찾을 수 없습니다.");
        verify(userRepository).getOrThrow(userId);
    }
}