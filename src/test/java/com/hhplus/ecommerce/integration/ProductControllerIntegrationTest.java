package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.config.EmbeddedRedisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ContextConfiguration(initializers = EmbeddedRedisConfig.class)
@DisplayName("ProductController 통합 테스트")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("상품 목록 조회 성공")
    void getProducts_Success() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_Success() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").exists())
                .andExpect(jsonPath("$.options").isArray());
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 실패")
    void getProductDetail_NotFound_Fail() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 99999L))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("인기 상품 조회 성공")
    void getPopularProducts_Success() throws Exception {
        mockMvc.perform(get("/api/products/popular")
                        .param("limit", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.period").exists());
    }
}
