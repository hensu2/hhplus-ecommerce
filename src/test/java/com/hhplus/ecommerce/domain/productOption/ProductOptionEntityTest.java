package com.hhplus.ecommerce.domain.productOption;

import com.hhplus.ecommerce.common.exception.InvalidStockUpdateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionEntityTest {

    private ProductOptionEntity testOption;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();
        testOption = new ProductOptionEntity(1L, 1L, "색상:블랙", 0L, 100L, now, now);
    }

    @Test
    @DisplayName("SET 타입으로 재고를 특정 값으로 설정할 수 있다")
    void updateStock_Set() {
        // when
        ProductOptionEntity updated = testOption.updateStock(StockUpdateType.SET, 50);

        // then
        assertThat(updated.getStock()).isEqualTo(50);
        assertThat(updated.getId()).isEqualTo(testOption.getId());
        assertThat(updated.getProductId()).isEqualTo(testOption.getProductId());
    }

    @Test
    @DisplayName("INCREASE 타입으로 재고를 증가시킬 수 있다")
    void updateStock_Increase() {
        // when
        ProductOptionEntity updated = testOption.updateStock(StockUpdateType.INCREASE, 30);

        // then
        assertThat(updated.getStock()).isEqualTo(130);
    }

    @Test
    @DisplayName("DECREASE 타입으로 재고를 감소시킬 수 있다")
    void updateStock_Decrease() {
        // when
        ProductOptionEntity updated = testOption.updateStock(StockUpdateType.DECREASE, 30);

        // then
        assertThat(updated.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고가 부족하면 DECREASE 시 예외가 발생한다")
    void updateStock_DecreaseWithInsufficientStock() {
        // when & then
        assertThatThrownBy(() -> testOption.updateStock(StockUpdateType.DECREASE, 150))
            .isInstanceOf(InvalidStockUpdateException.class)
            .hasMessageContaining("재고가 부족합니다")
            .hasMessageContaining("현재 재고: 100");
    }

    @Test
    @DisplayName("재고를 0으로 설정할 수 있다")
    void updateStock_SetToZero() {
        // when
        ProductOptionEntity updated = testOption.updateStock(StockUpdateType.SET, 0);

        // then
        assertThat(updated.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고를 정확히 0으로 감소시킬 수 있다")
    void updateStock_DecreaseToZero() {
        // when
        ProductOptionEntity updated = testOption.updateStock(StockUpdateType.DECREASE, 100);

        // then
        assertThat(updated.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("updatedAt이 업데이트된다")
    void updateStock_UpdatesTimestamp() throws InterruptedException {
        // given
        long beforeUpdate = testOption.getUpdatedAt();
        Thread.sleep(10);

        // when
        ProductOptionEntity updated = testOption.updateStock(StockUpdateType.SET, 50);

        // then
        assertThat(updated.getUpdatedAt()).isGreaterThan(beforeUpdate);
    }
}