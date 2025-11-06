package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.infrastructure.user.User;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserUseCase 단위 테스트")
class GetUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserUseCase getUserUseCase;

    private User testUser;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testUser = new User(1L, "testuser", 50000L, "USER", timestamp, timestamp);
    }

    @Test
    @DisplayName("유효한 사용자 ID로 사용자를 조회한다")
    void execute_ValidUserId_ReturnsUserResponse() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // when
        UserResponse response = getUserUseCase.execute(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testUser.id());
        assertThat(response.getUsername()).isEqualTo(testUser.username());
        assertThat(response.getPoint()).isEqualTo(testUser.point());
        assertThat(response.getRole()).isEqualTo(testUser.role());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("null 사용자 ID는 예외를 발생시킨다")
    void execute_NullUserId_ThrowsException() {
        // given
        Long nullId = null;

        // when & then
        assertThatThrownBy(() -> getUserUseCase.execute(nullId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("0 이하의 사용자 ID는 예외를 발생시킨다")
    void execute_InvalidUserId_ThrowsException() {
        // given
        Long invalidId = 0L;

        // when & then
        assertThatThrownBy(() -> getUserUseCase.execute(invalidId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User ID must be greater than 0");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID는 예외를 발생시킨다")
    void execute_UserNotFound_ThrowsException() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getUserUseCase.execute(userId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("사용자를 찾을 수 없습니다.");
        verify(userRepository).findById(userId);
    }
}