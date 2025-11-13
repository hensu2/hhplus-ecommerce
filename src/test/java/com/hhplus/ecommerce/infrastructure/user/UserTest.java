package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserEntity 도메인 모델 테스트")
class UserTest {

    @Test
    @DisplayName("유효한 User ID는 검증을 통과한다")
    void validateUserId_Success() {
        // given
        Long validId = 1L;

        // when & then
        assertThatCode(() -> UserEntity.validateUserId(validId))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null User ID는 검증 실패한다")
    void validateUserId_Null_ThrowsException() {
        // given
        Long nullId = null;

        // when & then
        assertThatThrownBy(() -> UserEntity.validateUserId(nullId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("0 이하의 User ID는 검증 실패한다")
    void validateUserId_ZeroOrNegative_ThrowsException() {
        // given
        Long zeroId = 0L;
        Long negativeId = -1L;

        // when & then
        assertThatThrownBy(() -> UserEntity.validateUserId(zeroId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("User ID must be greater than 0");

        assertThatThrownBy(() -> UserEntity.validateUserId(negativeId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("User ID must be greater than 0");
    }

    @Test
    @DisplayName("UserEntity를 UserResponse로 변환한다")
    void toUserResponse_Success() {
        // given
        UserEntity user = UserEntity.create("testuser", 50000L, "USER");

        // when
        UserResponse response = user.toUserResponse();

        // then
        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.point()).isEqualTo(50000L);
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("포인트를 충전한다")
    void chargePoint_Success() {
        // given
        UserEntity user = UserEntity.create("testuser", 50000L, "USER");

        // when
        user.chargePoint(10000L);

        // then
        assertThat(user.getPoint()).isEqualTo(60000L);
    }

    @Test
    @DisplayName("0 이하의 금액으로 충전시 예외 발생")
    void chargePoint_InvalidAmount_ThrowsException() {
        // given
        UserEntity user = UserEntity.create("testuser", 50000L, "USER");

        // when & then
        assertThatThrownBy(() -> user.chargePoint(0L))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("포인트를 사용한다")
    void usePoint_Success() {
        // given
        UserEntity user = UserEntity.create("testuser", 50000L, "USER");

        // when
        user.usePoint(10000L);

        // then
        assertThat(user.getPoint()).isEqualTo(40000L);
    }

    @Test
    @DisplayName("보유 포인트보다 많이 사용시 예외 발생")
    void usePoint_InsufficientPoint_ThrowsException() {
        // given
        UserEntity user = UserEntity.create("testuser", 50000L, "USER");

        // when & then
        assertThatThrownBy(() -> user.usePoint(60000L))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("포인트가 부족합니다.");
    }
}