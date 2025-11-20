package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductEntity> productPage = new PageImpl<>(testProducts, pageable, testProducts.size());
        given(productRepository.findAll(any(Pageable.class))).willReturn(productPage);

        // when
        Page<ProductEntity> response = getProductsUseCase.execute(pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getTotalElements()).isEqualTo(3);

        assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(response.getContent().get(0).getProductName()).isEqualTo("노트북");
        assertThat(response.getContent().get(0).getContent()).isEqualTo("고성능 노트북");
        assertThat(response.getContent().get(0).getPrice()).isEqualTo(890000L);

        assertThat(response.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(response.getContent().get(1).getProductName()).isEqualTo("키보드");

        assertThat(response.getContent().get(2).getId()).isEqualTo(3L);
        assertThat(response.getContent().get(2).getProductName()).isEqualTo("마우스");

        verify(productRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("상품이 없을 경우 빈 페이지를 반환한다")
    void execute_NoProducts_ReturnsEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(productRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

        // when
        Page<ProductEntity> response = getProductsUseCase.execute(pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        verify(productRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("페이징된 상품 목록을 조회한다")
    void execute_WithPagination() {
        // given
        long timestamp = System.currentTimeMillis();
        ProductEntity singleProduct = new ProductEntity(1L, 1L, "테스트 상품", "테스트 설명", 50000L, timestamp, timestamp);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductEntity> productPage = new PageImpl<>(List.of(singleProduct), pageable, 1);
        given(productRepository.findAll(any(Pageable.class))).willReturn(productPage);

        // when
        Page<ProductEntity> response = getProductsUseCase.execute(pageable);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);

        ProductEntity product = response.getContent().get(0);
        assertThat(product.getId()).isEqualTo(singleProduct.getId());
        assertThat(product.getProductName()).isEqualTo(singleProduct.getProductName());
        assertThat(product.getContent()).isEqualTo(singleProduct.getContent());
        assertThat(product.getPrice()).isEqualTo(singleProduct.getPrice());

        verify(productRepository).findAll(any(Pageable.class));
    }
}
