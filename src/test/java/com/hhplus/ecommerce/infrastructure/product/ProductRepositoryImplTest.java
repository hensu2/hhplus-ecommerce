package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.infrastructure.product.memory.ProductTable;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRepositoryImpl 단위 테스트")
class ProductRepositoryImplTest {

    @Mock
    private ProductTable productTable;

    @InjectMocks
    private ProductRepositoryImpl productRepository;

    @Test
    @DisplayName("전체 상품 목록을 조회한다")
    void findAll_ReturnsAllProducts() {
        // given
        long timestamp = System.currentTimeMillis();
        List<ProductEntity> products = Arrays.asList(
            new ProductEntity(1L, 1L, "노트북", "고성능 노트북", 890000L, timestamp, timestamp),
            new ProductEntity(2L, 1L, "키보드", "기계식 키보드", 120000L, timestamp, timestamp),
            new ProductEntity(3L, 1L, "마우스", "게이밍 마우스", 85000L, timestamp, timestamp)
        );
        given(productTable.findAll()).willReturn(products);

        // when
        List<ProductEntity> result = productRepository.findAll();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo(products);
        verify(productTable).findAll();
    }

    @Test
    @DisplayName("상품이 없을 경우 빈 리스트를 반환한다")
    void findAll_NoProducts_ReturnsEmptyList() {
        // given
        given(productTable.findAll()).willReturn(List.of());

        // when
        List<ProductEntity> result = productRepository.findAll();

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(productTable).findAll();
    }

    @Test
    @DisplayName("ID로 상품을 조회한다")
    void findById_ExistingProduct_ReturnsProduct() {
        // given
        long timestamp = System.currentTimeMillis();
        Long productId = 1L;
        ProductEntity product = new ProductEntity(1L, 1L, "노트북", "고성능 노트북", 890000L, timestamp, timestamp);
        given(productTable.findById(productId)).willReturn(Optional.of(product));

        // when
        Optional<ProductEntity> result = productRepository.findById(productId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(product);
        verify(productTable).findById(productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 조회하면 빈 Optional을 반환한다")
    void findById_NonExistingProduct_ReturnsEmpty() {
        // given
        Long productId = 999L;
        given(productTable.findById(productId)).willReturn(Optional.empty());

        // when
        Optional<ProductEntity> result = productRepository.findById(productId);

        // then
        assertThat(result).isEmpty();
        verify(productTable).findById(productId);
    }
}
