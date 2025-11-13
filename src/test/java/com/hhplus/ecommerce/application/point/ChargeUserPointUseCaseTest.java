package com.hhplus.ecommerce.application.point;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.PointHistoryRepository;
import com.hhplus.ecommerce.domain.point.TransactionType;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.domain.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.res.ChargePointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ChargeUserPointUseCase 단위 테스트
 * - Mock을 사용하여 비즈니스 로직만 검증
 * - Repository 의존성 제거
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeUserPointUseCase 단위 테스트")
class ChargeUserPointUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private ChargeUserPointUseCase chargeUserPointUseCase;

    private UserEntity testUser;

    @BeforeEach
    void setUp() throws Exception {
        testUser = UserEntity.create("testUser", 10000L, "USER");
        // Reflection으로 ID 설정
        Field idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 1L);
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint_Success() {
        // given
        Long userId = 1L;
        Integer chargeAmount = 5000;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(pointHistoryRepository.save(any(PointHistoryEntity.class))).thenReturn(mock(PointHistoryEntity.class));

        // when
        ChargePointResponse response = chargeUserPointUseCase.execute(userId, chargeAmount);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.amount()).isEqualTo(chargeAmount);
        assertThat(response.transactionType()).isEqualTo("EARN");
        assertThat(response.afterBalance()).isEqualTo(15000);

        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserEntity.class));
        verify(pointHistoryRepository).save(any(PointHistoryEntity.class));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 최소 금액 미만")
    void chargePoint_Fail_BelowMinimum() {
        // given
        Long userId = 1L;
        Integer chargeAmount = 500; // 최소 1000원 미만

        // when & then
        assertThatThrownBy(() -> chargeUserPointUseCase.execute(userId, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 금액은 1,000원 이상이어야 합니다");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트 충전 실패 - 사용자 없음")
    void chargePoint_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        Integer chargeAmount = 5000;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chargeUserPointUseCase.execute(userId, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트 충전 성공 - 정확히 최소 금액")
    void chargePoint_Success_ExactlyMinimum() {
        // given
        Long userId = 1L;
        Integer chargeAmount = 1000; // 정확히 최소 금액

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(pointHistoryRepository.save(any(PointHistoryEntity.class))).thenReturn(mock(PointHistoryEntity.class));

        // when
        ChargePointResponse response = chargeUserPointUseCase.execute(userId, chargeAmount);

        // then
        assertThat(response.amount()).isEqualTo(chargeAmount);
        assertThat(response.afterBalance()).isEqualTo(11000);

        verify(userRepository).save(any(UserEntity.class));
        verify(pointHistoryRepository).save(any(PointHistoryEntity.class));
    }

    @Test
    @DisplayName("포인트 충전 성공 - 큰 금액")
    void chargePoint_Success_LargeAmount() {
        // given
        Long userId = 1L;
        Integer chargeAmount = 1000000; // 100만원

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(pointHistoryRepository.save(any(PointHistoryEntity.class))).thenReturn(mock(PointHistoryEntity.class));

        // when
        ChargePointResponse response = chargeUserPointUseCase.execute(userId, chargeAmount);

        // then
        assertThat(response.amount()).isEqualTo(chargeAmount);
        assertThat(response.afterBalance()).isEqualTo(1010000);
    }
}
