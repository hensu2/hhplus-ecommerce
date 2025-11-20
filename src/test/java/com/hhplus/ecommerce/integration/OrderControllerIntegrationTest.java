package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("OrderController 통합 테스트")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() throws Exception {
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(1L, 2)
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, items, null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 1));
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("주문 완료 성공")
    void completeOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 1));
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/complete", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("존재하지 않는 주문 취소 실패")
    void cancelOrder_NotFound_Fail() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 99999L))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("존재하지 않는 주문 완료 실패")
    void completeOrder_NotFound_Fail() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/complete", 99999L))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }
}
