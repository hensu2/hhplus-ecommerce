package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.common.exception.InvalidStockUpdateException;
import com.hhplus.ecommerce.common.exception.ProductOptionNotFoundException;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
import com.hhplus.ecommerce.presentation.productOption.res.UpdateStockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateStockUseCaseTest {

    @Mock
    private ProductOptionRepository productOptionRepository;

    @InjectMocks
    private UpdateStockUseCase updateStockUseCase;

    private ProductOptionEntity testOption;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();
        testOption = new ProductOptionEntity(1L, 1L, "색상:블랙", 0L, 100L, now, now);
    }

    @Test
    @DisplayName("재고를 특정 값으로 설정할 수 있다")
    void setStock() {
        // given
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 50);
        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(productOptionRepository.save(any(ProductOptionEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UpdateStockResponse result = updateStockUseCase.execute(1L, request);

        // then
        assertThat(result.getStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("재고를 증가시킬 수 있다")
    void increaseStock() {
        // given
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.INCREASE, 30);
        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(productOptionRepository.save(any(ProductOptionEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UpdateStockResponse result = updateStockUseCase.execute(1L, request);

        // then
        assertThat(result.getStock()).isEqualTo(130);
    }

    @Test
    @DisplayName("재고를 감소시킬 수 있다")
    void decreaseStock() {
        // given
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.DECREASE, 30);
        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(productOptionRepository.save(any(ProductOptionEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UpdateStockResponse result = updateStockUseCase.execute(1L, request);

        // then
        assertThat(result.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void decreaseStockWithInsufficientStock() {
        // given
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.DECREASE, 150);
        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption));

        // when & then
        assertThatThrownBy(() -> updateStockUseCase.execute(1L, request))
            .isInstanceOf(InvalidStockUpdateException.class)
            .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("존재하지 않는 옵션 ID로 재고를 업데이트하면 예외가 발생한다")
    void updateStockWithInvalidOptionId() {
        // given
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 50);
        when(productOptionRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateStockUseCase.execute(999L, request))
            .isInstanceOf(ProductOptionNotFoundException.class)
            .hasMessageContaining("옵션을 찾을 수 없습니다");
    }
}