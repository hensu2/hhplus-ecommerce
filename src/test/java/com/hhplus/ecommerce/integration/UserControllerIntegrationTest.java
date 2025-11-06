package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.user.req.CreateUserRequest;
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
@DisplayName("UserController 통합 테스트")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_Success() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest("testUser", 1000L, "USER");

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.point").value(1000))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("사용자 조회 성공")
    void getUser_Success() throws Exception {
        // given - 먼저 사용자 생성
        CreateUserRequest createRequest = new CreateUserRequest("testUser", 1000L, "USER");
        String createResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = objectMapper.readTree(createResponse).get("id").asLong();

        // when & then
        mockMvc.perform(get("/api/users/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.point").value(1000))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
    void getUser_NotFound() throws Exception {
        // given
        Long nonExistentUserId = 99999L;

        // when & then
        mockMvc.perform(get("/api/users/{id}", nonExistentUserId))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 성공")
    void getUsers_Success() throws Exception {
        // given - 여러 사용자 생성
        CreateUserRequest user1 = new CreateUserRequest("user1", 1000L, "USER");
        CreateUserRequest user2 = new CreateUserRequest("user2", 2000L, "ADMIN");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].username", hasItem("user1")))
                .andExpect(jsonPath("$[*].username", hasItem("user2")));
    }

    @Test
    @DisplayName("빈 목록 조회 성공")
    void getUsers_EmptyList() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("사용자 생성 후 조회 확인")
    void createUserAndGet_Success() throws Exception {
        // given
        CreateUserRequest createRequest = new CreateUserRequest("integrationTestUser", 5000L, "ADMIN");

        // when - 사용자 생성
        String createResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = objectMapper.readTree(createResponse).get("id").asLong();

        // then - 생성된 사용자 조회
        mockMvc.perform(get("/api/users/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("integrationTestUser"))
                .andExpect(jsonPath("$.point").value(5000))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("여러 사용자 생성 후 목록에서 모두 조회 확인")
    void createMultipleUsersAndGetAll_Success() throws Exception {
        // given - 3명의 사용자 생성
        String[] usernames = {"testUser1", "testUser2", "testUser3"};

        for (String username : usernames) {
            CreateUserRequest request = new CreateUserRequest(username, 1000L, "USER");
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // when & then - 전체 목록 조회
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].username", hasItem("testUser1")))
                .andExpect(jsonPath("$[*].username", hasItem("testUser2")))
                .andExpect(jsonPath("$[*].username", hasItem("testUser3")));
    }
}