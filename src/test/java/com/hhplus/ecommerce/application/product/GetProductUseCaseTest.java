package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.common.exception.InvalidInputException;
import com.hhplus.ecommerce.common.exception.ProductNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductStatisticsRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.product.res.ProductDetailResponse;
import com.hhplus.ecommerce.presentation.product.res.ProductOptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
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
    private ProductOptionRepository productOptionRepository;

    @Mock
    private ProductStatisticsRepository productStatisticsRepository;

    @InjectMocks
    private GetProductUseCase getProductUseCase;

    private ProductEntity testProduct;
    private List<ProductOptionEntity> testOptions;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testProduct = new ProductEntity(1L, 1L, "노트북", "고성능 노트북", 890000L, timestamp, timestamp);

        testOptions = Arrays.asList(
            new ProductOptionEntity(1L, 1L, "16GB RAM", 100000L, 50L, timestamp, timestamp),
            new ProductOptionEntity(2L, 1L, "32GB RAM", 200000L, 30L, timestamp, timestamp)
        );
    }

    @Test
    @DisplayName("유효한 상품 ID로 상품 상세 정보를 조회한다")
    void execute_ValidProductId_ReturnsProductDetail() {
        // given
        Long productId = 1L;
        ProductStatisticsEntity existingStats = new ProductStatisticsEntity(productId, 10L, 5L, System.currentTimeMillis());

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productOptionRepository.findByProductId(productId)).thenReturn(testOptions);
        when(productStatisticsRepository.findByProductId(productId)).thenReturn(Optional.of(existingStats));
        when(productStatisticsRepository.save(any())).thenReturn(existingStats);

        // when
        ProductDetailResponse response = getProductUseCase.execute(productId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testProduct.getId());
        assertThat(response.productName()).isEqualTo(testProduct.getProductName());
        assertThat(response.content()).isEqualTo(testProduct.getContent());
        assertThat(response.price()).isEqualTo((int) testProduct.getPrice().longValue());
        assertThat(response.options()).hasSize(2);

        List<ProductOptionResponse> options = response.options();
        assertThat(options.get(0).id()).isEqualTo(1L);
        assertThat(options.get(0).optionType()).isEqualTo("16GB RAM");
        assertThat(options.get(0).additionalPrice()).isEqualTo(100000);
        assertThat(options.get(0).stock()).isEqualTo(50);
    }

    @Test
    @DisplayName("옵션이 없는 상품의 상세 정보를 조회한다")
    void execute_ProductWithNoOptions_ReturnsProductDetailWithEmptyOptions() {
        // given
        Long productId = 1L;
        ProductStatisticsEntity existingStats = new ProductStatisticsEntity(productId, 10L, 5L, System.currentTimeMillis());

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productOptionRepository.findByProductId(productId)).thenReturn(List.of());
        when(productStatisticsRepository.findByProductId(productId)).thenReturn(Optional.of(existingStats));
        when(productStatisticsRepository.save(any())).thenReturn(existingStats);

        // when
        ProductDetailResponse response = getProductUseCase.execute(productId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testProduct.getId());
        assertThat(response.productName()).isEqualTo(testProduct.getProductName());
        assertThat(response.options()).isEmpty();
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
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getProductUseCase.execute(productId))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("상품을 찾을 수 없습니다.");
    }
}
