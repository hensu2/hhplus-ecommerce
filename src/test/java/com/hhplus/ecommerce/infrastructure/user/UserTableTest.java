package com.hhplus.ecommerce.infrastructure.user;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.memory.UserTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserTable 단위 테스트")
class UserTableTest {

    private UserTable userTable;

    @BeforeEach
    void setUp() {
        userTable = new UserTable();
    }

    @Test
    @DisplayName("전체 사용자 목록을 조회한다")
    void findAll_ReturnsAllUsers() {
        // when
        List<UserEntity> users = userTable.findAll();

        // then
        assertThat(users).isNotNull();
        assertThat(users).hasSize(3);
        assertThat(users).extracting(UserEntity::id).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(users).extracting(UserEntity::username).containsExactlyInAnyOrder("user123", "admin", "testuser");
    }

    @Test
    @DisplayName("반환된 리스트는 불변이다")
    void findAll_ReturnsImmutableList() {
        // when
        List<UserEntity> users = userTable.findAll();

        // then
        assertThatThrownBy(() -> users.add(new UserEntity(4L, "newuser", 10000L, "USER", 0L, 0L)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("ID로 사용자를 조회한다")
    void findById_ExistingUser_ReturnsUser() {
        // given
        Long userId = 1L;

        // when
        Optional<UserEntity> result = userTable.findById(userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().username()).isEqualTo("user123");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void findById_NonExistingUser_ReturnsEmpty() {
        // given
        Long userId = 999L;

        // when
        Optional<UserEntity> result = userTable.findById(userId);

        // then
        assertThat(result).isEmpty();
    }
}
