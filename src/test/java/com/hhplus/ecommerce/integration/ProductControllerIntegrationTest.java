package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProductController 통합 테스트")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품 목록 조회 성공 - 기본 페이징")
    void getProducts_DefaultPaging_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.number").exists());
    }

    @Test
    @DisplayName("상품 목록 조회 성공 - 커스텀 페이징")
    void getProducts_CustomPaging_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("상품 목록 조회 - 상품 구조 검증")
    void getProducts_ProductStructure_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].id").exists())
                .andExpect(jsonPath("$.content[*].productName").exists())
                .andExpect(jsonPath("$.content[*].content").exists())
                .andExpect(jsonPath("$.content[*].price").exists())
                .andExpect(jsonPath("$.content[*].createdAt").exists());
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_Success() throws Exception {
        // given - 상품 목록에서 첫 번째 상품 ID 가져오기
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long productId = objectMapper.readTree(productsResponse)
                .get("content")
                .get(0)
                .get("id")
                .asLong();

        // when & then
        mockMvc.perform(get("/api/products/{id}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.productName").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("상품 상세 조회 - 옵션 구조 검증")
    void getProductDetail_OptionStructure_Success() throws Exception {
        // given - 상품 목록에서 첫 번째 상품 ID 가져오기
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long productId = objectMapper.readTree(productsResponse)
                .get("content")
                .get(0)
                .get("id")
                .asLong();

        // when & then
        mockMvc.perform(get("/api/products/{id}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options[*].id").exists())
                .andExpect(jsonPath("$.options[*].optionType").exists())
                .andExpect(jsonPath("$.options[*].additionalPrice").exists())
                .andExpect(jsonPath("$.options[*].stock").exists());
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
    void getProductDetail_NotFound() throws Exception {
        // given
        Long nonExistentProductId = 99999L;

        // when & then
        mockMvc.perform(get("/api/products/{id}", nonExistentProductId))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 기본 개수")
    void getPopularProducts_DefaultLimit_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.period").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    @DisplayName("인기 상품 조회 성공 - 커스텀 개수")
    void getPopularProducts_CustomLimit_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/popular")
                        .param("limit", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products", hasSize(lessThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("인기 상품 조회 - 인기 상품 구조 검증")
    void getPopularProducts_Structure_Success() throws Exception {
        // when
        String response = mockMvc.perform(get("/api/products/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.period").value("조회수 + 판매량 기준"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 인기 상품이 있는 경우에만 구조 검증
        var products = objectMapper.readTree(response).get("products");
        if (products.size() > 0) {
            mockMvc.perform(get("/api/products/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products[0].productId").exists())
                    .andExpect(jsonPath("$.products[0].productName").exists())
                    .andExpect(jsonPath("$.products[0].price").exists())
                    .andExpect(jsonPath("$.products[0].viewCount").exists())
                    .andExpect(jsonPath("$.products[0].salesCount").exists())
                    .andExpect(jsonPath("$.products[0].popularityScore").exists());
        }
    }

    @Test
    @DisplayName("인기 상품 조회 - 인기도 점수 내림차순 정렬 확인")
    void getPopularProducts_SortedByScore_Success() throws Exception {
        // when
        String response = mockMvc.perform(get("/api/products/popular")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 인기도 점수가 내림차순으로 정렬되어 있는지 확인
        var products = objectMapper.readTree(response).get("products");
        if (products.size() > 1) {
            for (int i = 0; i < products.size() - 1; i++) {
                long currentScore = products.get(i).get("popularityScore").asLong();
                long nextScore = products.get(i + 1).get("popularityScore").asLong();
                assert currentScore >= nextScore : "인기도 점수가 내림차순으로 정렬되어야 합니다";
            }
        }
    }

    @Test
    @DisplayName("상품 목록과 상세 조회 데이터 일관성 확인")
    void getProductsAndDetail_Consistency_Success() throws Exception {
        // given - 상품 목록 조회
        String productsResponse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var content = objectMapper.readTree(productsResponse).get("content");

        // 상품이 있는 경우에만 테스트
        if (content.size() > 0) {
            var firstProduct = content.get(0);
            Long productId = firstProduct.get("id").asLong();
            int price = firstProduct.get("price").asInt();

            // when & then - 상세 조회 시 동일한 데이터 확인
            mockMvc.perform(get("/api/products/{id}", productId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productId))
                    .andExpect(jsonPath("$.productName").exists())
                    .andExpect(jsonPath("$.price").value(price));
        }
    }

    @Test
    @DisplayName("인기 상품이 일반 상품 목록에도 존재하는지 확인")
    void popularProductsExistInProductList_Success() throws Exception {
        // given - 인기 상품 조회
        String popularResponse = mockMvc.perform(get("/api/products/popular")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var popularProducts = objectMapper.readTree(popularResponse).get("products");
        if (popularProducts.size() > 0) {
            Long popularProductId = popularProducts.get(0).get("productId").asLong();

            // when & then - 해당 상품이 상세 조회 가능한지 확인
            mockMvc.perform(get("/api/products/{id}", popularProductId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(popularProductId));
        }
    }

    @Test
    @DisplayName("여러 페이지 조회 시 페이지 정보 확인")
    void getProductsMultiplePages_Success() throws Exception {
        // when & then - 첫 번째 페이지
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5));

        // when & then - 두 번째 페이지
        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @DisplayName("인기 상품 조회 - 빈 결과 처리")
    void getPopularProducts_EmptyResult_Success() throws Exception {
        // when & then - limit이 0이더라도 응답 구조는 유지되어야 함
        mockMvc.perform(get("/api/products/popular")
                        .param("limit", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.period").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    @DisplayName("상품 가격 정보 확인 - 양수 값")
    void getProducts_PriceValidation_Success() throws Exception {
        // when
        String response = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 모든 상품의 가격이 양수인지 확인
        var products = objectMapper.readTree(response).get("content");
        for (var product : products) {
            int price = product.get("price").asInt();
            assert price > 0 : "상품 가격은 양수여야 합니다";
        }
    }
}
