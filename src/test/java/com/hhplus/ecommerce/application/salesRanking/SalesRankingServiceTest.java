package com.hhplus.ecommerce.application.salesRanking;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.salesRanking.SalesRankingItem;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.salesRanking.SalesRankingRepository;
import com.hhplus.ecommerce.presentation.salesRanking.res.SalesRankingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesRankingServiceTest {

    @Mock
    private SalesRankingRepository salesRankingRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SalesRankingService salesRankingService;

    @Test
    @DisplayName("판매 랭킹 증가 - 금일 및 금주 랭킹 업데이트")
    void increaseRanking() {
        // given
        Long productId = 1L;
        Integer quantity = 5;
        Long orderDate = System.currentTimeMillis();

        // when
        salesRankingService.increaseRanking(productId, quantity, orderDate);

        // then
        verify(salesRankingRepository).incrementScore(contains("sales:ranking:daily:"), eq(productId), eq(quantity));
        verify(salesRankingRepository).setExpire(contains("sales:ranking:daily:"), eq(26L), eq(TimeUnit.HOURS));
        verify(salesRankingRepository).incrementScore(contains("sales:ranking:weekly:"), eq(productId), eq(quantity));
        verify(salesRankingRepository).setExpire(contains("sales:ranking:weekly:"), eq(8L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("판매 랭킹 감소 - 금일 및 금주 랭킹 차감")
    void decreaseRanking() {
        // given
        Long productId = 1L;
        Integer quantity = 3;
        Long cancelDate = System.currentTimeMillis();

        // when
        salesRankingService.decreaseRanking(productId, quantity, cancelDate);

        // then
        verify(salesRankingRepository).decrementScore(contains("sales:ranking:daily:"), eq(productId), eq(quantity));
        verify(salesRankingRepository).decrementScore(contains("sales:ranking:weekly:"), eq(productId), eq(quantity));
    }

    @Test
    @DisplayName("금일 판매 랭킹 조회 - 페이징 및 상품 정보 조합")
    void getDailySalesRanking() {
        // given
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int page = 0;
        int size = 10;

        List<SalesRankingItem> mockRankings = Arrays.asList(
            new SalesRankingItem(1, 101L, 50L),
            new SalesRankingItem(2, 102L, 30L)
        );

        ProductEntity product1 = new ProductEntity(101L, 1L, "상품1", null, 10000L, System.currentTimeMillis(), System.currentTimeMillis());
        ProductEntity product2 = new ProductEntity(102L, 1L, "상품2", null, 20000L, System.currentTimeMillis(), System.currentTimeMillis());

        when(salesRankingRepository.getRankingWithScores(anyString(), eq(0L), eq(9L)))
            .thenReturn(mockRankings);
        when(salesRankingRepository.getSize(anyString())).thenReturn(2L);
        when(productRepository.getOrThrow(101L)).thenReturn(product1);
        when(productRepository.getOrThrow(102L)).thenReturn(product2);

        // when
        SalesRankingResponse response = salesRankingService.getDailySalesRanking(date, page, size);

        // then
        assertThat(response.getType()).isEqualTo("daily");
        assertThat(response.getRankings()).hasSize(2);
        assertThat(response.getRankings().get(0).getProductName()).isEqualTo("상품1");
        assertThat(response.getRankings().get(0).getSalesCount()).isEqualTo(50L);
        assertThat(response.getTotalCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("금주 판매 랭킹 조회 - 페이징 및 상품 정보 조합")
    void getWeeklySalesRanking() {
        // given
        int year = 2025;
        int week = 49;
        int page = 0;
        int size = 10;

        List<SalesRankingItem> mockRankings = Arrays.asList(
            new SalesRankingItem(1, 101L, 500L),
            new SalesRankingItem(2, 102L, 300L)
        );

        ProductEntity product1 = new ProductEntity(101L, 1L, "상품1", null, 10000L, System.currentTimeMillis(), System.currentTimeMillis());
        ProductEntity product2 = new ProductEntity(102L, 1L, "상품2", null, 20000L, System.currentTimeMillis(), System.currentTimeMillis());

        when(salesRankingRepository.getRankingWithScores(anyString(), eq(0L), eq(9L)))
            .thenReturn(mockRankings);
        when(salesRankingRepository.getSize(anyString())).thenReturn(2L);
        when(productRepository.getOrThrow(101L)).thenReturn(product1);
        when(productRepository.getOrThrow(102L)).thenReturn(product2);

        // when
        SalesRankingResponse response = salesRankingService.getWeeklySalesRanking(year, week, page, size);

        // then
        assertThat(response.getType()).isEqualTo("weekly");
        assertThat(response.getPeriod()).isEqualTo("2025-W49");
        assertThat(response.getRankings()).hasSize(2);
        assertThat(response.getRankings().get(0).getSalesCount()).isEqualTo(500L);
        assertThat(response.getTotalCount()).isEqualTo(2L);
    }
}