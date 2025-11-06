package com.hhplus.ecommerce.infrastructure.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryImpl 단위 테스트")
class UserRepositoryImplTest {

    @Mock
    private UserTable userTable;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testUser = new User(1L, "testuser", 50000L, "USER", timestamp, timestamp);
    }

    @Test
    @DisplayName("ID로 사용자를 조회한다")
    void findById_ExistingUser_ReturnsUser() {
        // given
        Long userId = 1L;
        given(userTable.findById(userId)).willReturn(Optional.of(testUser));

        // when
        Optional<User> result = userRepository.findById(userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userTable).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회하면 빈 Optional을 반환한다")
    void findById_NonExistingUser_ReturnsEmpty() {
        // given
        Long userId = 999L;
        given(userTable.findById(userId)).willReturn(Optional.empty());

        // when
        Optional<User> result = userRepository.findById(userId);

        // then
        assertThat(result).isEmpty();
        verify(userTable).findById(userId);
    }
}