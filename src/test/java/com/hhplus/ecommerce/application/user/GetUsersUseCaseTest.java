package com.hhplus.ecommerce.application.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import com.hhplus.ecommerce.presentation.user.res.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUsersUseCase 단위 테스트")
class GetUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUsersUseCase getUsersUseCase;

    private List<UserEntity> testUsers;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testUsers = Arrays.asList(
            new UserEntity(1L, "user1", 50000L, "USER", timestamp, timestamp),
            new UserEntity(2L, "user2", 100000L, "ADMIN", timestamp, timestamp),
            new UserEntity(3L, "user3", 30000L, "USER", timestamp, timestamp)
        );
    }

    @Test
    @DisplayName("전체 사용자 목록을 조회한다")
    void execute_ReturnsAllUsers() {
        // given
        given(userRepository.findAll()).willReturn(testUsers);

        // when
        List<UserResponse> responses = getUsersUseCase.execute();

        // then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(0).getUsername()).isEqualTo("user1");
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        assertThat(responses.get(1).getUsername()).isEqualTo("user2");
        assertThat(responses.get(2).getId()).isEqualTo(3L);
        assertThat(responses.get(2).getUsername()).isEqualTo("user3");
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("사용자가 없을 경우 빈 리스트를 반환한다")
    void execute_NoUsers_ReturnsEmptyList() {
        // given
        given(userRepository.findAll()).willReturn(List.of());

        // when
        List<UserResponse> responses = getUsersUseCase.execute();

        // then
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();
        verify(userRepository).findAll();
    }
}
