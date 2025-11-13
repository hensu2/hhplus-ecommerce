package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.presentation.product.res.ProductDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * GetProductUseCase 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductUseCase 단위 테스트")
class GetProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductUseCase getProductUseCase;

    private ProductEntity testProduct;
    private UserEntity testUser;

    @BeforeEach
    void setUp() throws Exception {
        testUser = UserEntity.create("testUser", 10000L, "USER");

        // Reflection으로 User ID 설정
        Field userIdField = UserEntity.class.getDeclaredField("id");
        userIdField.setAccessible(true);
        userIdField.set(testUser, 1L);

        testProduct = ProductEntity.create(testUser, "테스트 상품", "테스트 상품 설명", 10000L);

        // Reflection으로 Product ID 설정
        Field productIdField = ProductEntity.class.getDeclaredField("id");
        productIdField.setAccessible(true);
        productIdField.set(testProduct, 1L);
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProduct_Success() {
        // given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // when
        ProductDetailResponse response = getProductUseCase.execute(productId);

        // then
        assertThat(response.productName()).isEqualTo("테스트 상품");
        assertThat(response.content()).isEqualTo("테스트 상품 설명");
        assertThat(response.price()).isEqualTo(10000L);

        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("상품 조회 실패 - 상품 없음")
    void getProduct_Fail_ProductNotFound() {
        // given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getProductUseCase.execute(productId))
                .hasMessageContaining("상품을 찾을 수 없습니다");

        verify(productRepository).findById(productId);
    }
}
