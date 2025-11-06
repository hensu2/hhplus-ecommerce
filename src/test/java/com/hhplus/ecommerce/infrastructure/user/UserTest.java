package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 도메인 모델 테스트")
class UserTest {

    @Test
    @DisplayName("유효한 User ID는 검증을 통과한다")
    void validateUserId_Success() {
        // given
        Long validId = 1L;

        // when & then
        assertThatCode(() -> User.validateUserId(validId))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null User ID는 검증 실패한다")
    void validateUserId_Null_ThrowsException() {
        // given
        Long nullId = null;

        // when & then
        assertThatThrownBy(() -> User.validateUserId(nullId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("0 이하의 User ID는 검증 실패한다")
    void validateUserId_ZeroOrNegative_ThrowsException() {
        // given
        Long zeroId = 0L;
        Long negativeId = -1L;

        // when & then
        assertThatThrownBy(() -> User.validateUserId(zeroId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User ID must be greater than 0");

        assertThatThrownBy(() -> User.validateUserId(negativeId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User ID must be greater than 0");
    }

    @Test
    @DisplayName("User를 UserResponse로 변환한다")
    void toUserResponse_Success() {
        // given
        long timestamp = System.currentTimeMillis();
        User user = new User(1L, "testuser", 50000L, "USER", timestamp, timestamp);

        // when
        UserResponse response = user.toUserResponse();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getPoint()).isEqualTo(50000L);
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getCreatedAt()).isEqualTo(timestamp);
        assertThat(response.getUpdatedAt()).isEqualTo(timestamp);
    }
}