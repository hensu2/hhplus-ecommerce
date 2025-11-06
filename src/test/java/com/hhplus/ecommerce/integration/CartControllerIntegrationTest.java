package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.cart.req.AddToCartRequest;
import com.hhplus.ecommerce.presentation.cart.req.UpdateCartItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("CartController 통합 테스트")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("장바구니 조회 성공")
    void getCart_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/cart"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @DisplayName("장바구니 추가 성공")
    void addToCart_Success() throws Exception {
        // given
        AddToCartRequest request = new AddToCartRequest(1L, 1L, 2);

        // when & then
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").exists())
                .andExpect(jsonPath("$.optionId").value(1))
                .andExpect(jsonPath("$.optionType").exists())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unitPrice").exists())
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.addedAt").exists());
    }

    @Test
    @DisplayName("장바구니 추가 실패 - 재고 부족")
    void addToCart_InsufficientStock() throws Exception {
        // given - 재고보다 많은 수량
        AddToCartRequest request = new AddToCartRequest(1L, 1L, 1000);

        // when & then
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("장바구니 수량 변경 성공")
    void updateCartItem_Success() throws Exception {
        // given - 먼저 장바구니에 상품 추가
        AddToCartRequest addRequest = new AddToCartRequest(1L, 1L, 2);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse).get("id").asLong();

        // when - 수량 변경
        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest(5);

        // then
        mockMvc.perform(put("/api/cart/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("장바구니 수량 변경 실패 - 존재하지 않는 아이템")
    void updateCartItem_NotFound() throws Exception {
        // given
        Long nonExistentCartItemId = 99999L;
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        // when & then
        mockMvc.perform(put("/api/cart/{id}", nonExistentCartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("장바구니 삭제 성공")
    void deleteCartItem_Success() throws Exception {
        // given - 장바구니에 상품 추가
        AddToCartRequest addRequest = new AddToCartRequest(1L, 1L, 1);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse).get("id").asLong();

        // when & then
        mockMvc.perform(delete("/api/cart/{id}", cartItemId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("장바구니 추가 후 조회 확인")
    void addToCartAndGet_Success() throws Exception {
        // given - 장바구니에 상품 추가
        AddToCartRequest request = new AddToCartRequest(1L, 1L, 3);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse).get("id").asLong();

        // when & then - 장바구니 조회
        mockMvc.perform(get("/api/cart"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.cartItemId == " + cartItemId + ")].quantity").value(3));
    }

    @Test
    @DisplayName("장바구니 삭제 후 조회 확인")
    void deleteCartItemAndGet_Success() throws Exception {
        // given - 장바구니에 상품 추가
        AddToCartRequest request = new AddToCartRequest(1L, 1L, 2);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse).get("id").asLong();

        // when - 삭제
        mockMvc.perform(delete("/api/cart/{id}", cartItemId))
                .andExpect(status().isNoContent());

        // then - 조회 시 삭제된 아이템 없음
        String cartResponse = mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var items = objectMapper.readTree(cartResponse).get("items");
        boolean found = false;
        for (var item : items) {
            if (item.get("cartItemId").asLong() == cartItemId) {
                found = true;
                break;
            }
        }
        assert !found : "삭제된 아이템이 조회되면 안됩니다";
    }

    @Test
    @DisplayName("여러 상품 장바구니 추가 후 총액 확인")
    void addMultipleItemsAndCheckTotal_Success() throws Exception {
        // given - 여러 상품 추가
        AddToCartRequest item1 = new AddToCartRequest(1L, 1L, 2);
        AddToCartRequest item2 = new AddToCartRequest(2L, 2L, 1);

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item2)))
                .andExpect(status().isCreated());

        // when & then - 총액 확인
        mockMvc.perform(get("/api/cart"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @DisplayName("장바구니 수량 여러 번 변경 성공")
    void updateCartItemMultipleTimes_Success() throws Exception {
        // given - 장바구니에 상품 추가
        AddToCartRequest addRequest = new AddToCartRequest(1L, 1L, 1);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse).get("id").asLong();

        // when - 수량을 여러 번 변경
        mockMvc.perform(put("/api/cart/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3));

        mockMvc.perform(put("/api/cart/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));

        mockMvc.perform(put("/api/cart/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(2))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    @DisplayName("장바구니 수량 변경 후 총액 재계산 확인")
    void updateCartItemAndCheckTotalPrice_Success() throws Exception {
        // given - 장바구니에 상품 추가
        AddToCartRequest addRequest = new AddToCartRequest(1L, 1L, 2);
        String addResponse = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse).get("id").asLong();
        int unitPrice = objectMapper.readTree(addResponse).get("unitPrice").asInt();

        // when - 수량을 10으로 변경
        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest(10);
        mockMvc.perform(put("/api/cart/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.totalPrice").value(unitPrice * 10));
    }
}
