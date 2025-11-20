package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
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
@DisplayName("PaymentController 통합 테스트")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("결제 처리 성공")
    void processPayment_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 1));
        CreateOrderRequest orderRequest = new CreateOrderRequest(1L, items, null);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("orderId").asLong();
        Integer finalAmount = objectMapper.readTree(orderResponse).get("finalAmount").asInt();

        // when & then - 결제 처리
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest(1L, orderId, finalAmount);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.amount").value(finalAmount))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("결제 목록 조회 성공")
    void getPayments_Success() throws Exception {
        mockMvc.perform(get("/api/payments")
                        .param("userId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments").isArray());
    }

    @Test
    @DisplayName("결제 취소 성공")
    void cancelPayment_Success() throws Exception {
        // given - 주문 및 결제 생성
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 1));
        CreateOrderRequest orderRequest = new CreateOrderRequest(1L, items, null);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("orderId").asLong();
        Integer finalAmount = objectMapper.readTree(orderResponse).get("finalAmount").asInt();

        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest(1L, orderId, finalAmount);
        String paymentResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long paymentId = objectMapper.readTree(paymentResponse).get("paymentId").asLong();

        // when & then - 결제 취소
        mockMvc.perform(post("/api/payments/{paymentId}/cancel", paymentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("존재하지 않는 결제 취소 실패")
    void cancelPayment_NotFound_Fail() throws Exception {
        mockMvc.perform(post("/api/payments/{paymentId}/cancel", 99999L))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }
}
