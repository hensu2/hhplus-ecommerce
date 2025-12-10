package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.common.exception.ProductNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductUseCase 단위 테스트")
class GetProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStatisticsRepository productStatisticsRepository;

    @InjectMocks
    private GetProductUseCase getProductUseCase;

    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testProduct = new ProductEntity(1L, 1L, "노트북", "고성능 노트북", 890000L, timestamp, timestamp);
    }

    @Test
    @DisplayName("유효한 상품 ID로 상품 정보를 조회한다")
    void execute_ValidProductId_ReturnsProduct() {
        // given
        Long productId = 1L;
        ProductStatisticsEntity existingStats = new ProductStatisticsEntity(productId, 10L, 5L, System.currentTimeMillis());

        when(productRepository.getOrThrow(productId)).thenReturn(testProduct);
        when(productStatisticsRepository.findByProductId(productId)).thenReturn(Optional.of(existingStats));
        when(productStatisticsRepository.save(any())).thenReturn(existingStats);

        // when
        ProductEntity result = getProductUseCase.execute(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testProduct.getId());
        assertThat(result.getProductName()).isEqualTo(testProduct.getProductName());
        assertThat(result.getContent()).isEqualTo(testProduct.getContent());
        assertThat(result.getPrice()).isEqualTo(testProduct.getPrice());
    }

    @Test
    @DisplayName("상품 조회 시 조회수가 증가한다")
    void execute_ProductView_IncreasesViewCount() {
        // given
        Long productId = 1L;
        ProductStatisticsEntity existingStats = new ProductStatisticsEntity(productId, 10L, 5L, System.currentTimeMillis());

        when(productRepository.getOrThrow(productId)).thenReturn(testProduct);
        when(productStatisticsRepository.findByProductId(productId)).thenReturn(Optional.of(existingStats));
        when(productStatisticsRepository.save(any())).thenReturn(existingStats);

        // when
        ProductEntity result = getProductUseCase.execute(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testProduct.getId());
        assertThat(result.getProductName()).isEqualTo(testProduct.getProductName());
    }

    @Test
    @DisplayName("null 상품 ID는 예외를 발생시킨다")
    void execute_NullProductId_ThrowsException() {
        // given
        Long nullId = null;

        // when & then
        assertThatThrownBy(() -> getProductUseCase.execute(nullId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("Product ID cannot be null");
    }

    @Test
    @DisplayName("0 이하의 상품 ID는 예외를 발생시킨다")
    void execute_InvalidProductId_ThrowsException() {
        // given
        Long invalidId = 0L;

        // when & then
        assertThatThrownBy(() -> getProductUseCase.execute(invalidId))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("Product ID must be greater than 0");
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID는 예외를 발생시킨다")
    void execute_ProductNotFound_ThrowsException() {
        // given
        Long productId = 999L;
        when(productRepository.getOrThrow(productId)).thenThrow(new ProductNotFoundException("상품을 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> getProductUseCase.execute(productId))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("상품을 찾을 수 없습니다.");
    }
}
