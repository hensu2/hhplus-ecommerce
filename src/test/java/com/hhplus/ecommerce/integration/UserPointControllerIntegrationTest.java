package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.user.req.ChargePointRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("UserPointController 통합 테스트")
class UserPointControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("포인트 조회 성공")
    void getPoint_Success() throws Exception {
        mockMvc.perform(get("/api/users/me/point")
                        .param("userId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.point").exists());
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint_Success() throws Exception {
        ChargePointRequest request = new ChargePointRequest(10000);

        mockMvc.perform(post("/api/users/me/point/charge")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.afterBalance").exists())
                .andExpect(jsonPath("$.transactionType").value("EARN"));
    }

    @Test
    @DisplayName("포인트 사용 이력 조회 성공")
    void getPointHistory_Success() throws Exception {
        mockMvc.perform(get("/api/users/me/point/history")
                        .param("userId", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 조회 실패")
    void getPoint_NotFound_Fail() throws Exception {
        mockMvc.perform(get("/api/users/me/point")
                        .param("userId", "99999"))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }
}
