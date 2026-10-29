package com.example.schedulebook.domain.user.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserRole;
import com.example.schedulebook.domain.user.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이미 존재하는 로그인 ID인지 확인한다")
    void existsByLoginId_존재하는_로그인_ID를_확인한다() {
        // given
        User user = createUser("testLogin");

        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByLoginId("testLogin");
        boolean notExists = userRepository.existsByLoginId("unknownLogin");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("로그인 ID로 사용자를 조회한다")
    void findByLoginId_로그인_ID로_사용자를_조회한다() {
        // given
        User user = createUser("testLogin");

        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByLoginId("testLogin");
        Optional<User> notFoundUser = userRepository.findByLoginId("unknownLogin");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getLoginId()).isEqualTo("testLogin");

        assertThat(notFoundUser).isEmpty();
    }

    @Test
    @DisplayName("이미 존재하는 이메일인지 확인한다")
    void existsByEmail_존재하는_이메일을_확인한다() {
        // given
        User user = createUser("testLogin");

        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByEmail("testLogin@example.com");
        boolean notExists = userRepository.existsByEmail("unknown@example.com");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("이미 존재하는 닉네임인지 확인한다")
    void existsByNickname_존재하는_닉네임을_확인한다() {
        // given
        User user = createUser("testLogin");

        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByNickname("testLoginNickname");
        boolean notExists = userRepository.existsByNickname("unknownNickname");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("이미 존재하는 전화번호인지 확인한다")
    void existsByPhoneNumber_존재하는_전화번호를_확인한다() {
        // given
        User user = createUser("testLogin");

        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByPhoneNumber(createPhoneNumber("testLogin"));
        boolean notExists = userRepository.existsByPhoneNumber("010-9999-9999");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("활성 상태이며 삭제되지 않은 관리자만 조회한다")
    void findAllActiveAdmins_활성_관리자만_조회한다() {
        // given
        User activeAdmin = createUser("activeAdmin");
        User inactiveAdmin = createUser("inactiveAdmin");
        User deletedAdmin = createUser("deletedAdmin");
        User activeUser = createUser("activeUser");

        setUserRole(activeAdmin, UserRole.SUPER_ADMIN);
        setUserRole(inactiveAdmin, UserRole.SUPER_ADMIN);
        setUserRole(deletedAdmin, UserRole.SUPER_ADMIN);

        setUserStatus(inactiveAdmin, UserStatus.DENIED);

        deletedAdmin.withdraw("encodedPassword");

        userRepository.saveAll(
                List.of(activeAdmin, inactiveAdmin, deletedAdmin, activeUser)
        );

        // when
        List<User> admins = userRepository.findAllActiveAdmins(UserRole.SUPER_ADMIN);

        // then
        assertThat(admins)
                .extracting(User::getLoginId)
                .containsExactly("activeAdmin");

        assertThat(admins)
                .allSatisfy(admin -> {
                    assertThat(admin.getUserRole()).isEqualTo(UserRole.SUPER_ADMIN);
                    assertThat(admin.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(admin.isDeleted()).isFalse();
                });
    }

    @Test
    @DisplayName("활성 관리자 조회 시 일반 사용자 권한은 반환하지 않는다")
    void findAllActiveAdmins_일반_사용자는_조회하지_않는다() {
        // given
        User activeUser = createUser("activeUser");

        userRepository.save(activeUser);

        // when
        List<User> admins = userRepository.findAllActiveAdmins(UserRole.SUPER_ADMIN);

        // then
        assertThat(admins).isEmpty();
    }

    private User createUser(String loginId) {
        return User.create(
                loginId,
                "encodedPassword",
                loginId + "Nickname",
                loginId + "@example.com",
                createPhoneNumber(loginId)
        );
    }

    private String createPhoneNumber(String loginId) {
        // 각 사용자에게 고유한 전화번호를 부여한다.
        int suffix = Math.abs(loginId.hashCode() % 1_000_000);

        return String.format("010-%04d-%04d",
                suffix / 10_000,
                suffix % 10_000);
    }

    private void setUserRole(User user, UserRole userRole) {
        try {
            var field = User.class.getDeclaredField("userRole");
            field.setAccessible(true);
            field.set(user, userRole);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setUserStatus(User user, UserStatus userStatus) {
        try {
            var field = User.class.getDeclaredField("userStatus");
            field.setAccessible(true);
            field.set(user, userStatus);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}