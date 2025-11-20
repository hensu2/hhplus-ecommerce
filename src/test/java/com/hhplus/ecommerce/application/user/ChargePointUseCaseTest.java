package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.PointHistoryEntity;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.PointHistoryRepository;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargePointUseCase 단위 테스트")
class ChargePointUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private ChargePointUseCase chargePointUseCase;

    @Test
    @DisplayName("포인트 충전 성공")
    void execute_Success() {
        // given
        Long userId = 1L;
        Long chargeAmount = 10000L;
        Long initialPoint = 5000L;
        Long expectedPoint = 15000L;

        UserEntity user = new UserEntity(userId, "testuser", initialPoint, "USER", 0L, 0L);
        UserEntity updatedUser = new UserEntity(userId, "testuser", expectedPoint, "USER", 0L, 0L);

        when(userRepository.getOrThrow(userId)).thenReturn(user);
        when(userRepository.save(any(UserEntity.class))).thenReturn(updatedUser);
        when(pointHistoryRepository.save(any(PointHistoryEntity.class))).thenReturn(any(PointHistoryEntity.class));

        // when
        UserEntity result = chargePointUseCase.execute(userId, chargeAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPoint()).isEqualTo(expectedPoint);
        verify(userRepository, times(1)).getOrThrow(userId);
        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistoryEntity.class));
    }
}
