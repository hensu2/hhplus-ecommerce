package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import com.hhplus.ecommerce.presentation.product.res.PopularProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPopularProductsUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStatisticsRepository productStatisticsRepository;

    @InjectMocks
    private GetPopularProductsUseCase getPopularProductsUseCase;

    @Test
    @DisplayName("인기도 점수 기준으로 상위 N개 상품을 정렬하여 반환한다")
    void shouldReturnTopNProductsSortedByPopularityScore() {
        // given
        ProductStatisticsEntity stats1 = new ProductStatisticsEntity(1L, 100, 5, System.currentTimeMillis()); // score: 150
        ProductStatisticsEntity stats2 = new ProductStatisticsEntity(2L, 50, 10, System.currentTimeMillis()); // score: 150
        ProductStatisticsEntity stats3 = new ProductStatisticsEntity(3L, 200, 3, System.currentTimeMillis()); // score: 230

        long now = System.currentTimeMillis();
        ProductEntity product1 = new ProductEntity(1L, 1L, "상품1", "설명1", 10000, now, now);
        ProductEntity product3 = new ProductEntity(3L, 1L, "상품3", "설명3", 30000, now, now);

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats1, stats2, stats3));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product3));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        // when
        List<PopularProductResponse> result = getPopularProductsUseCase.execute(2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(3L);
        assertThat(result.get(0).getPopularityScore()).isEqualTo(230L);
        assertThat(result.get(1).getPopularityScore()).isIn(150L);
    }

    @Test
    @DisplayName("조회수와 판매량을 기준으로 인기도 점수를 계산한다")
    void shouldCalculatePopularityScoreCorrectly() {
        // given
        ProductStatisticsEntity stats = new ProductStatisticsEntity(1L, 100, 10, System.currentTimeMillis());
        long now = System.currentTimeMillis();
        ProductEntity product = new ProductEntity(1L, 1L, "상품1", "설명1", 10000, now, now);

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        List<PopularProductResponse> result = getPopularProductsUseCase.execute(1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getViewCount()).isEqualTo(100L);
        assertThat(result.get(0).getSalesCount()).isEqualTo(10L);
        assertThat(result.get(0).getPopularityScore()).isEqualTo(200L); // (100 * 1) + (10 * 10)
    }

    @Test
    @DisplayName("limit 파라미터만큼의 상품만 반환한다")
    void shouldReturnLimitedNumberOfProducts() {
        // given
        ProductStatisticsEntity stats1 = new ProductStatisticsEntity(1L, 100, 5, System.currentTimeMillis());
        ProductStatisticsEntity stats2 = new ProductStatisticsEntity(2L, 200, 10, System.currentTimeMillis());
        ProductStatisticsEntity stats3 = new ProductStatisticsEntity(3L, 300, 15, System.currentTimeMillis());

        long now = System.currentTimeMillis();
        ProductEntity product2 = new ProductEntity(2L, 1L, "상품2", "설명2", 20000, now, now);
        ProductEntity product3 = new ProductEntity(3L, 1L, "상품3", "설명3", 30000, now, now);

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats1, stats2, stats3));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product3));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));

        // when
        List<PopularProductResponse> result = getPopularProductsUseCase.execute(2);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("상품이 존재하지 않는 통계는 필터링한다")
    void shouldFilterOutStatisticsWithoutProduct() {
        // given
        ProductStatisticsEntity stats1 = new ProductStatisticsEntity(1L, 100, 5, System.currentTimeMillis());
        ProductStatisticsEntity stats2 = new ProductStatisticsEntity(2L, 200, 10, System.currentTimeMillis());

        long now = System.currentTimeMillis();
        ProductEntity product1 = new ProductEntity(1L, 1L, "상품1", "설명1", 10000, now, now);

        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList(stats1, stats2));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        // when
        List<PopularProductResponse> result = getPopularProductsUseCase.execute(5);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("통계가 없으면 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoStatistics() {
        // given
        when(productStatisticsRepository.findAll()).thenReturn(Arrays.asList());

        // when
        List<PopularProductResponse> result = getPopularProductsUseCase.execute(5);

        // then
        assertThat(result).isEmpty();
    }
}
