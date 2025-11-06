package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import com.hhplus.ecommerce.infrastructure.product.memory.ProductStatisticsTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncreaseProductViewUseCaseTest {

    @Mock
    private ProductStatisticsRepository productStatisticsRepository;

    @Mock
    private ProductStatisticsTable productStatisticsTable;

    @InjectMocks
    private IncreaseProductViewUseCase increaseProductViewUseCase;

    @Test
    @DisplayName("기존 통계가 있으면 조회수를 1 증가시킨다")
    void shouldIncreaseViewCountWhenStatisticsExist() {
        // given
        long productId = 1L;
        ProductStatisticsEntity existingStats = new ProductStatisticsEntity(productId, 10, 5, System.currentTimeMillis());
        ProductStatisticsEntity updatedStats = existingStats.increaseViewCount();

        when(productStatisticsTable.getOrCreateDefault(productId)).thenReturn(existingStats);
        when(productStatisticsRepository.save(any())).thenReturn(updatedStats);

        // when
        increaseProductViewUseCase.execute(productId);

        // then
        verify(productStatisticsTable).getOrCreateDefault(productId);
        verify(productStatisticsRepository).save(any(ProductStatisticsEntity.class));
    }

    @Test
    @DisplayName("통계가 없으면 새로 생성하고 조회수를 1로 설정한다")
    void shouldCreateNewStatisticsWhenNotExist() {
        // given
        long productId = 1L;
        ProductStatisticsEntity newStats = new ProductStatisticsEntity(productId, 0, 0, System.currentTimeMillis());
        ProductStatisticsEntity updatedStats = newStats.increaseViewCount();

        when(productStatisticsTable.getOrCreateDefault(productId)).thenReturn(newStats);
        when(productStatisticsRepository.save(any())).thenReturn(updatedStats);

        // when
        increaseProductViewUseCase.execute(productId);

        // then
        verify(productStatisticsTable).getOrCreateDefault(productId);
        verify(productStatisticsRepository).save(any(ProductStatisticsEntity.class));
    }

    @Test
    @DisplayName("조회수 증가 후 저장소에 저장한다")
    void shouldSaveAfterIncreasingViewCount() {
        // given
        long productId = 1L;
        ProductStatisticsEntity stats = new ProductStatisticsEntity(productId, 5, 3, System.currentTimeMillis());
        ProductStatisticsEntity updatedStats = stats.increaseViewCount();

        when(productStatisticsTable.getOrCreateDefault(productId)).thenReturn(stats);
        when(productStatisticsRepository.save(any())).thenReturn(updatedStats);

        // when
        increaseProductViewUseCase.execute(productId);

        // then
        verify(productStatisticsRepository).save(any(ProductStatisticsEntity.class));
    }
}
