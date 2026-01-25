package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncreaseProductViewUseCaseTest {

    @Mock
    private ProductStatisticsRepository productStatisticsRepository;

    @InjectMocks
    private IncreaseProductViewUseCase increaseProductViewUseCase;

    @Test
    @DisplayName("기존 통계가 있으면 조회수를 1 증가시킨다")
    void shouldIncreaseViewCountWhenStatisticsExist() {
        // given
        long productId = 1L;
        ProductStatisticsEntity existingStats = new ProductStatisticsEntity(productId, 10L, 5L, System.currentTimeMillis());

        when(productStatisticsRepository.findByProductId(productId)).thenReturn(Optional.of(existingStats));
        when(productStatisticsRepository.save(any())).thenReturn(existingStats);

        // when
        increaseProductViewUseCase.execute(productId);

        // then
        verify(productStatisticsRepository).findByProductId(productId);
        verify(productStatisticsRepository).save(any(ProductStatisticsEntity.class));
    }

    @Test
    @DisplayName("통계가 없으면 새로 생성하고 조회수를 1로 설정한다")
    void shouldCreateNewStatisticsWhenNotExist() {
        // given
        long productId = 1L;
        ProductStatisticsEntity newStats = new ProductStatisticsEntity(productId, 0L, 0L, System.currentTimeMillis());

        when(productStatisticsRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(productStatisticsRepository.save(any())).thenReturn(newStats);

        // when
        increaseProductViewUseCase.execute(productId);

        // then
        verify(productStatisticsRepository).findByProductId(productId);
        verify(productStatisticsRepository).save(any(ProductStatisticsEntity.class));
    }

    @Test
    @DisplayName("조회수 증가 후 저장소에 저장한다")
    void shouldSaveAfterIncreasingViewCount() {
        // given
        long productId = 1L;
        ProductStatisticsEntity existingStats = new ProductStatisticsEntity(productId, 5L, 3L, System.currentTimeMillis());

        when(productStatisticsRepository.findByProductId(productId)).thenReturn(Optional.of(existingStats));
        when(productStatisticsRepository.save(any())).thenReturn(existingStats);

        // when
        increaseProductViewUseCase.execute(productId);

        // then
        verify(productStatisticsRepository).save(any(ProductStatisticsEntity.class));
    }
}
