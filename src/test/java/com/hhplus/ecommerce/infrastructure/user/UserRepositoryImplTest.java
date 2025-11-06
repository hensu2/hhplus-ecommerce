package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.memory.UserTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
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

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testUser = new UserEntity(1L, "testuser", 50000L, "USER", timestamp, timestamp);
    }

    @Test
    @DisplayName("ID로 사용자를 조회한다")
    void findById_ExistingUser_ReturnsUser() {
        // given
        Long userId = 1L;
        given(userTable.findById(userId)).willReturn(Optional.of(testUser));

        // when
        Optional<UserEntity> result = userRepository.findById(userId);

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
        Optional<UserEntity> result = userRepository.findById(userId);

        // then
        assertThat(result).isEmpty();
        verify(userTable).findById(userId);
    }

    @Test
    @DisplayName("전체 사용자 목록을 조회한다")
    void findAll_ReturnsAllUsers() {
        // given
        long timestamp = System.currentTimeMillis();
        List<UserEntity> users = Arrays.asList(
            new UserEntity(1L, "user1", 50000L, "USER", timestamp, timestamp),
            new UserEntity(2L, "user2", 100000L, "ADMIN", timestamp, timestamp),
            new UserEntity(3L, "user3", 30000L, "USER", timestamp, timestamp)
        );
        given(userTable.findAll()).willReturn(users);

        // when
        List<UserEntity> result = userRepository.findAll();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo(users);
        verify(userTable).findAll();
    }

    @Test
    @DisplayName("사용자가 없을 경우 빈 리스트를 반환한다")
    void findAll_NoUsers_ReturnsEmptyList() {
        // given
        given(userTable.findAll()).willReturn(List.of());

        // when
        List<UserEntity> result = userRepository.findAll();

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(userTable).findAll();
    }

    @Test
    @DisplayName("새로운 사용자를 저장한다")
    void save_NewUser_ReturnsSavedUser() {
        // given
        long timestamp = System.currentTimeMillis();
        UserEntity newUser = new UserEntity(0L, "newuser", 10000L, "USER", 0L, 0L);
        UserEntity savedUser = new UserEntity(4L, "newuser", 10000L, "USER", timestamp, timestamp);
        given(userTable.save(newUser)).willReturn(savedUser);

        // when
        UserEntity result = userRepository.save(newUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(4L);
        assertThat(result.username()).isEqualTo("newuser");
        assertThat(result.point()).isEqualTo(10000L);
        assertThat(result.role()).isEqualTo("USER");
        verify(userTable).save(newUser);
    }
}