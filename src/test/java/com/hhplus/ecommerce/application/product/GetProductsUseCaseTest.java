package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.presentation.product.res.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductsUseCase 단위 테스트")
class GetProductsUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductsUseCase getProductsUseCase;

    private List<ProductEntity> testProducts;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testProducts = Arrays.asList(
            new ProductEntity(1L, 1L, "노트북", "고성능 노트북", 890000L, timestamp, timestamp),
            new ProductEntity(2L, 1L, "키보드", "기계식 키보드", 120000L, timestamp, timestamp),
            new ProductEntity(3L, 1L, "마우스", "게이밍 마우스", 85000L, timestamp, timestamp)
        );
    }

    @Test
    @DisplayName("전체 상품 목록을 조회한다")
    void execute_ReturnsAllProducts() {
        // given
        given(productRepository.findAll()).willReturn(testProducts);

        // when
        List<ProductResponse> response = getProductsUseCase.execute();

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(3);

        assertThat(response.get(0).getId()).isEqualTo(1L);
        assertThat(response.get(0).getProductName()).isEqualTo("노트북");
        assertThat(response.get(0).getContent()).isEqualTo("고성능 노트북");
        assertThat(response.get(0).getPrice()).isEqualTo(890000);

        assertThat(response.get(1).getId()).isEqualTo(2L);
        assertThat(response.get(1).getProductName()).isEqualTo("키보드");

        assertThat(response.get(2).getId()).isEqualTo(3L);
        assertThat(response.get(2).getProductName()).isEqualTo("마우스");

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("상품이 없을 경우 빈 리스트를 반환한다")
    void execute_NoProducts_ReturnsEmptyList() {
        // given
        given(productRepository.findAll()).willReturn(List.of());

        // when
        List<ProductResponse> response = getProductsUseCase.execute();

        // then
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("상품 목록이 ProductResponse로 올바르게 변환된다")
    void execute_ConvertsToProductResponse() {
        // given
        long timestamp = System.currentTimeMillis();
        ProductEntity singleProduct = new ProductEntity(1L, 1L, "테스트 상품", "테스트 설명", 50000L, timestamp, timestamp);
        given(productRepository.findAll()).willReturn(List.of(singleProduct));

        // when
        List<ProductResponse> response = getProductsUseCase.execute();

        // then
        assertThat(response).hasSize(1);
        ProductResponse productResponse = response.get(0);
        assertThat(productResponse.getId()).isEqualTo(singleProduct.id());
        assertThat(productResponse.getProductName()).isEqualTo(singleProduct.productName());
        assertThat(productResponse.getContent()).isEqualTo(singleProduct.content());
        assertThat(productResponse.getPrice()).isEqualTo((int) singleProduct.price());
        assertThat(productResponse.getCreatedAt()).isNotNull();

        verify(productRepository).findAll();
    }
}
