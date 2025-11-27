package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPopularProductsUseCaseTest {

    @Mock
    private ProductStatisticsRepository productStatisticsRepository;

    @InjectMocks
    private GetPopularProductsUseCase getPopularProductsUseCase;

    @Test
    @DisplayName("전날 판매량 기준으로 상위 N개 상품을 정렬하여 반환한다")
    void shouldReturnTopNProductsSortedBySalesCount() {
        // given
        // 조회수는 0, 판매량만 반영 (전날 집계 데이터)
        // Repository의 findAll()은 이미 판매량 순으로 정렬되어 반환됨
        ProductStatisticsEntity stats2 = new ProductStatisticsEntity(2L, 0L, 10L, System.currentTimeMillis());
        ProductStatisticsEntity stats1 = new ProductStatisticsEntity(1L, 0L, 5L, System.currentTimeMillis());
        ProductStatisticsEntity stats3 = new ProductStatisticsEntity(3L, 0L, 3L, System.currentTimeMillis());

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats2, stats1, stats3));

        // when
        List<ProductStatisticsEntity> result = getPopularProductsUseCase.execute(2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(2L); // 판매량 10
        assertThat(result.get(0).getSalesCount()).isEqualTo(10L);
        assertThat(result.get(1).getProductId()).isEqualTo(1L); // 판매량 5
        assertThat(result.get(1).getSalesCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("전날 판매량만 반영하여 인기상품을 반환한다")
    void shouldReturnProductsBasedOnSalesCountOnly() {
        // given
        ProductStatisticsEntity stats = new ProductStatisticsEntity(1L, 0L, 10L, System.currentTimeMillis());

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats));

        // when
        List<ProductStatisticsEntity> result = getPopularProductsUseCase.execute(1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getViewCount()).isEqualTo(0L); // 조회수는 인기상품 순위에 미반영
        assertThat(result.get(0).getSalesCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("limit 파라미터만큼의 상품만 반환한다")
    void shouldReturnLimitedNumberOfProducts() {
        // given
        ProductStatisticsEntity stats3 = new ProductStatisticsEntity(3L, 0L, 15L, System.currentTimeMillis());
        ProductStatisticsEntity stats2 = new ProductStatisticsEntity(2L, 0L, 10L, System.currentTimeMillis());
        ProductStatisticsEntity stats1 = new ProductStatisticsEntity(1L, 0L, 5L, System.currentTimeMillis());

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats3, stats2, stats1));

        // when
        List<ProductStatisticsEntity> result = getPopularProductsUseCase.execute(2);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("판매량 기준으로 정렬된 통계를 반환한다")
    void shouldReturnSortedStatistics() {
        // given
        ProductStatisticsEntity stats2 = new ProductStatisticsEntity(2L, 0L, 10L, System.currentTimeMillis());
        ProductStatisticsEntity stats1 = new ProductStatisticsEntity(1L, 0L, 5L, System.currentTimeMillis());

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats2, stats1));

        // when
        List<ProductStatisticsEntity> result = getPopularProductsUseCase.execute(5);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(2L);
        assertThat(result.get(1).getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("통계가 없으면 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoStatistics() {
        // given
        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList());

        // when
        List<ProductStatisticsEntity> result = getPopularProductsUseCase.execute(5);

        // then
        assertThat(result).isEmpty();
    }
}
