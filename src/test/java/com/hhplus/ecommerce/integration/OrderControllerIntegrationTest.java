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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("OrderController 통합 테스트")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() throws Exception {
        // given
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, items, null);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 적용")
    void createOrder_WithCoupon_Success() throws Exception {
        // given
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, items, 1L);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.couponHistoryId").value(1))
                .andExpect(jsonPath("$.discountAmount").exists())
                .andExpect(jsonPath("$.finalAmount").exists());
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1)
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when & then - 주문 취소
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
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1)
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when & then - 주문 완료
        mockMvc.perform(post("/api/orders/{orderId}/complete", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_InsufficientStock_Fail() throws Exception {
        // given - 재고보다 많은 수량
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 10000)
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, items, null);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("존재하지 않는 주문 취소 실패")
    void cancelOrder_NotFound_Fail() throws Exception {
        // given
        Long nonExistentOrderId = 99999L;

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", nonExistentOrderId))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("존재하지 않는 주문 완료 실패")
    void completeOrder_NotFound_Fail() throws Exception {
        // given
        Long nonExistentOrderId = 99999L;

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/complete", nonExistentOrderId))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("주문 생성 후 취소 플로우")
    void createAndCancelOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 2)
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when - 주문 취소
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("여러 상품 주문 생성 성공")
    void createOrderWithMultipleItems_Success() throws Exception {
        // given
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1),
                new OrderItemRequest(2L, 2),
                new OrderItemRequest(3L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, items, null);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @DisplayName("주문 아이템 구조 검증")
    void createOrder_ItemStructure_Success() throws Exception {
        // given
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, items, null);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].productOptionId").exists())
                .andExpect(jsonPath("$.items[0].quantity").exists())
                .andExpect(jsonPath("$.items[0].unitPrice").exists())
                .andExpect(jsonPath("$.items[0].totalPrice").exists());
    }

    @Test
    @DisplayName("쿠폰 적용 시 할인 금액 확인")
    void createOrder_CouponDiscount_Success() throws Exception {
        // given
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, items, 1L);

        // when
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 할인 금액 확인
        int totalAmount = objectMapper.readTree(response).get("totalAmount").asInt();
        int discountAmount = objectMapper.readTree(response).get("discountAmount").asInt();
        int finalAmount = objectMapper.readTree(response).get("finalAmount").asInt();

        assert finalAmount == totalAmount - discountAmount : "최종 금액 계산 오류";
        assert discountAmount > 0 : "할인 금액이 0보다 커야 함";
    }

    @Test
    @DisplayName("주문 생성 후 완료 플로우")
    void createAndCompleteOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1)
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when - 주문 완료
        mockMvc.perform(post("/api/orders/{orderId}/complete", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
