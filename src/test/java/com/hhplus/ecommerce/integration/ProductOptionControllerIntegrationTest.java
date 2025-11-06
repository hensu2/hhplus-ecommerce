package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProductOptionController 통합 테스트")
class ProductOptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품별 옵션 재고 조회 성공")
    void getProductStock_Success() throws Exception {
        // given - 상품 목록에서 첫 번째 상품 ID 가져오기
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var content = objectMapper.readTree(productsResponse).get("content");
        if (content.size() > 0) {
            Long productId = content.get(0).get("id").asLong();

            // when & then
            mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(productId))
                    .andExpect(jsonPath("$.productName").exists())
                    .andExpect(jsonPath("$.options").isArray());
        }
    }

    @Test
    @DisplayName("상품별 옵션 재고 조회 - 옵션 구조 검증")
    void getProductStock_OptionStructure_Success() throws Exception {
        // given - 상품 목록에서 첫 번째 상품 ID 가져오기
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var content = objectMapper.readTree(productsResponse).get("content");
        if (content.size() > 0) {
            Long productId = content.get(0).get("id").asLong();

            // when & then
            mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.options").isArray())
                    .andExpect(jsonPath("$.options[*].optionId").exists())
                    .andExpect(jsonPath("$.options[*].optionType").exists())
                    .andExpect(jsonPath("$.options[*].stock").exists())
                    .andExpect(jsonPath("$.options[*].additionalPrice").exists());
        }
    }

    @Test
    @DisplayName("존재하지 않는 상품 재고 조회 시 예외 발생")
    void getProductStock_NotFound() throws Exception {
        // given
        Long nonExistentProductId = 99999L;

        // when & then
        mockMvc.perform(get("/api/product-options/stock/{productId}", nonExistentProductId))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("재고 업데이트 성공 - SET")
    void updateStock_Set_Success() throws Exception {
        // given - 옵션 ID 가져오기
        Long optionId = getFirstOptionId();
        if (optionId != null) {
            UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 100);

            // when & then
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionId").value(optionId))
                    .andExpect(jsonPath("$.optionType").exists())
                    .andExpect(jsonPath("$.stock").value(100))
                    .andExpect(jsonPath("$.additionalPrice").exists());
        }
    }

    @Test
    @DisplayName("재고 업데이트 성공 - INCREASE")
    void updateStock_Increase_Success() throws Exception {
        // given - 옵션 ID 가져오기
        Long optionId = getFirstOptionId();
        if (optionId != null) {
            // 먼저 재고를 50으로 설정
            UpdateStockRequest setRequest = new UpdateStockRequest(StockUpdateType.SET, 50);
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(setRequest)))
                    .andExpect(status().isOk());

            // when - 재고 30 증가
            UpdateStockRequest increaseRequest = new UpdateStockRequest(StockUpdateType.INCREASE, 30);

            // then
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(increaseRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionId").value(optionId))
                    .andExpect(jsonPath("$.stock").value(80));
        }
    }

    @Test
    @DisplayName("재고 업데이트 성공 - DECREASE")
    void updateStock_Decrease_Success() throws Exception {
        // given - 옵션 ID 가져오기
        Long optionId = getFirstOptionId();
        if (optionId != null) {
            // 먼저 재고를 100으로 설정
            UpdateStockRequest setRequest = new UpdateStockRequest(StockUpdateType.SET, 100);
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(setRequest)))
                    .andExpect(status().isOk());

            // when - 재고 30 감소
            UpdateStockRequest decreaseRequest = new UpdateStockRequest(StockUpdateType.DECREASE, 30);

            // then
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(decreaseRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionId").value(optionId))
                    .andExpect(jsonPath("$.stock").value(70));
        }
    }

    @Test
    @DisplayName("재고 업데이트 실패 - 재고 부족 시 감소 불가")
    void updateStock_Decrease_InsufficientStock() throws Exception {
        // given - 옵션 ID 가져오기
        Long optionId = getFirstOptionId();
        if (optionId != null) {
            // 먼저 재고를 10으로 설정
            UpdateStockRequest setRequest = new UpdateStockRequest(StockUpdateType.SET, 10);
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(setRequest)))
                    .andExpect(status().isOk());

            // when - 재고보다 많은 수량 감소 시도
            UpdateStockRequest decreaseRequest = new UpdateStockRequest(StockUpdateType.DECREASE, 20);

            // then
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(decreaseRequest)))
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        }
    }

    @Test
    @DisplayName("재고 업데이트 실패 - 존재하지 않는 옵션")
    void updateStock_OptionNotFound() throws Exception {
        // given
        Long nonExistentOptionId = 99999L;
        UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 100);

        // when & then
        mockMvc.perform(patch("/api/product-options/{optionId}/stock", nonExistentOptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("재고 업데이트 후 조회 확인")
    void updateStockAndGet_Success() throws Exception {
        // given - 상품 상세에서 옵션 ID 가져오기
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var content = objectMapper.readTree(productsResponse).get("content");
        if (content.size() > 0) {
            Long productId = content.get(0).get("id").asLong();

            String stockResponse = mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            var options = objectMapper.readTree(stockResponse).get("options");
            if (options.size() > 0) {
                Long optionId = options.get(0).get("optionId").asLong();

                // when - 재고를 200으로 설정
                UpdateStockRequest setRequest = new UpdateStockRequest(StockUpdateType.SET, 200);
                mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(setRequest)))
                        .andExpect(status().isOk());

                // then - 재고 조회하여 확인
                mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.options[?(@.optionId == " + optionId + ")].stock").value(200));
            }
        }
    }

    @Test
    @DisplayName("여러 옵션 재고 일괄 업데이트")
    void updateMultipleOptionsStock_Success() throws Exception {
        // given - 상품의 모든 옵션 가져오기
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var content = objectMapper.readTree(productsResponse).get("content");
        if (content.size() > 0) {
            Long productId = content.get(0).get("id").asLong();

            String stockResponse = mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            var options = objectMapper.readTree(stockResponse).get("options");

            // when - 모든 옵션의 재고를 50으로 설정
            for (int i = 0; i < options.size(); i++) {
                Long optionId = options.get(i).get("optionId").asLong();
                UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 50);

                mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.stock").value(50));
            }

            // then - 모든 옵션 재고가 50인지 확인
            mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("재고 0으로 설정 성공")
    void updateStock_SetToZero_Success() throws Exception {
        // given
        Long optionId = getFirstOptionId();
        if (optionId != null) {
            UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 0);

            // when & then
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(0));
        }
    }

    @Test
    @DisplayName("재고 연속 업데이트 성공")
    void updateStock_Consecutive_Success() throws Exception {
        // given
        Long optionId = getFirstOptionId();
        if (optionId != null) {
            // when - 재고를 100으로 설정
            UpdateStockRequest setRequest = new UpdateStockRequest(StockUpdateType.SET, 100);
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(setRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(100));

            // 50 증가
            UpdateStockRequest increaseRequest = new UpdateStockRequest(StockUpdateType.INCREASE, 50);
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(increaseRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(150));

            // 30 감소
            UpdateStockRequest decreaseRequest = new UpdateStockRequest(StockUpdateType.DECREASE, 30);
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(decreaseRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(120));

            // 다시 200으로 설정
            UpdateStockRequest setRequest2 = new UpdateStockRequest(StockUpdateType.SET, 200);
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(setRequest2)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(200));
        }
    }

    @Test
    @DisplayName("대량 재고 설정 성공")
    void updateStock_LargeAmount_Success() throws Exception {
        // given
        Long optionId = getFirstOptionId();
        if (optionId != null) {
            UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 100000);

            // when & then
            mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(100000));
        }
    }

    @Test
    @DisplayName("재고 업데이트 후 상품 상세 조회 시 재고 반영 확인")
    void updateStockAndGetProductDetail_Success() throws Exception {
        // given - 상품과 옵션 ID 가져오기
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var content = objectMapper.readTree(productsResponse).get("content");
        if (content.size() > 0) {
            Long productId = content.get(0).get("id").asLong();

            String stockResponse = mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            var options = objectMapper.readTree(stockResponse).get("options");
            if (options.size() > 0) {
                Long optionId = options.get(0).get("optionId").asLong();

                // when - 재고를 999로 설정
                UpdateStockRequest request = new UpdateStockRequest(StockUpdateType.SET, 999);
                mockMvc.perform(patch("/api/product-options/{optionId}/stock", optionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

                // then - 상품 상세 조회 시 재고 확인
                mockMvc.perform(get("/api/products/{id}", productId))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.options[?(@.id == " + optionId + ")].stock").value(999));
            }
        }
    }

    // 헬퍼 메서드: 첫 번째 옵션 ID 가져오기
    private Long getFirstOptionId() throws Exception {
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var content = objectMapper.readTree(productsResponse).get("content");
        if (content.size() > 0) {
            Long productId = content.get(0).get("id").asLong();

            String stockResponse = mockMvc.perform(get("/api/product-options/stock/{productId}", productId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            var options = objectMapper.readTree(stockResponse).get("options");
            if (options.size() > 0) {
                return options.get(0).get("optionId").asLong();
            }
        }
        return null;
    }
}
