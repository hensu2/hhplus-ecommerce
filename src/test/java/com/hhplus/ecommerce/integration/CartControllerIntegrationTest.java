package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.config.EmbeddedRedisConfig;
import com.hhplus.ecommerce.presentation.cart.req.AddToCartRequest;
import com.hhplus.ecommerce.presentation.cart.req.UpdateCartItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
@DisplayName("CartController 통합 테스트")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("장바구니 조회 성공")
    void getCart_Success() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .param("userId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @DisplayName("장바구니 추가 성공")
    void addToCart_Success() throws Exception {
        AddToCartRequest request = new AddToCartRequest(1L, 1L, 1);

        mockMvc.perform(post("/api/cart")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    @DisplayName("장바구니 수량 변경 성공")
    void updateCartItem_Success() throws Exception {
        // given - 먼저 장바구니에 추가
        AddToCartRequest addRequest = new AddToCartRequest(1L, 1L, 1);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartId = objectMapper.readTree(addResponse).get("id").asLong();

        // when & then - 수량 변경
        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest(3);
        mockMvc.perform(put("/api/cart/{id}", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    @DisplayName("장바구니 삭제 성공")
    void deleteCartItem_Success() throws Exception {
        // given - 먼저 장바구니에 추가
        AddToCartRequest addRequest = new AddToCartRequest(1L, 1L, 1);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartId = objectMapper.readTree(addResponse).get("id").asLong();

        // when & then - 삭제
        mockMvc.perform(delete("/api/cart/{id}", cartId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}