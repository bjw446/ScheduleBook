package com.example.schedulebook.domain.user.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.dto.request.UpdateUserPasswordRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.dto.request.WithdrawUserRequest;
import com.example.schedulebook.domain.user.dto.response.UpdateUserResponse;
import com.example.schedulebook.domain.user.dto.response.UserResponse;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import com.example.schedulebook.domain.user.repository.UserRepository;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator userValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.create(
                "test123",
                "Password1!",
                "테스트유저",
                "test@example.com",
                "010-1234-5678"
        );

        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "level", 3);
        ReflectionTestUtils.setField(user, "exp", 50);
        ReflectionTestUtils.setField(user, "loginCount", 10);
        ReflectionTestUtils.setField(user, "loginStreak", 5);
        ReflectionTestUtils.setField(user, "scheduleCount", 20);
    }

    @Test
    void 내_프로필_조회_성공() {
        // given
        Long currentUserId = 1L;

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        // when
        UserResponse response =
                userService.findMyProfile(currentUserId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.nickname())
                .isEqualTo(user.getNickname());
        assertThat(response.level())
                .isEqualTo(user.getLevel());
        assertThat(response.exp())
                .isEqualTo(user.getExp());
        assertThat(response.requiredExp())
                .isEqualTo(user.getRequiredExp());
        assertThat(response.loginCount())
                .isEqualTo(user.getLoginCount());
        assertThat(response.loginStreak())
                .isEqualTo(user.getLoginStreak());
        assertThat(response.scheduleCount())
                .isEqualTo(user.getScheduleCount());

        then(userValidator).should()
                .validateActiveUser(currentUserId);
    }

    @Test
    void 내_프로필_수정_성공() {
        // given
        Long currentUserId = 1L;

        UpdateUserRequest request = new UpdateUserRequest(
                "수정유저",
                "updated@example.com",
                "010-9876-5432"
        );

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        // when
        UpdateUserResponse response =
                userService.updateMyProfile(request, currentUserId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.nickname())
                .isEqualTo("수정유저");
        assertThat(response.email())
                .isEqualTo("updated@example.com");
        assertThat(response.phoneNumber())
                .isEqualTo("010-9876-5432");

        assertThat(user.getNickname())
                .isEqualTo("수정유저");
        assertThat(user.getEmail())
                .isEqualTo("updated@example.com");
        assertThat(user.getPhoneNumber())
                .isEqualTo("010-9876-5432");

        then(userValidator).should()
                .validateActiveUser(currentUserId);

        then(userValidator).should()
                .validateDuplicate(request, user);
    }

    @Test
    void 내_프로필_수정_중복값이면_예외가_발생하고_수정하지_않는다() {
        // given
        Long currentUserId = 1L;

        UpdateUserRequest request = new UpdateUserRequest(
                "중복유저",
                "duplicate@example.com",
                "010-1111-2222"
        );

        RuntimeException exception =
                new RuntimeException("duplicate");

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        willThrow(exception)
                .given(userValidator)
                .validateDuplicate(request, user);

        String originalNickname = user.getNickname();
        String originalEmail = user.getEmail();
        String originalPhoneNumber = user.getPhoneNumber();

        // when & then
        assertThatThrownBy(() ->
                userService.updateMyProfile(request, currentUserId)
        )
                .isSameAs(exception);

        assertThat(user.getNickname())
                .isEqualTo(originalNickname);
        assertThat(user.getEmail())
                .isEqualTo(originalEmail);
        assertThat(user.getPhoneNumber())
                .isEqualTo(originalPhoneNumber);
    }

    @Test
    void 내_프로필_수정_비활성_사용자이면_검증에서_실패한다() {
        // given
        Long currentUserId = 1L;

        RuntimeException exception =
                new RuntimeException("inactive user");

        given(userValidator.validateActiveUser(currentUserId))
                .willThrow(exception);

        UpdateUserRequest request = new UpdateUserRequest(
                "수정유저",
                "updated@example.com",
                "010-9876-5432"
        );

        // when & then
        assertThatThrownBy(() ->
                userService.updateMyProfile(request, currentUserId)
        )
                .isSameAs(exception);

        then(userValidator).should(never())
                .validateDuplicate(any(), any());
    }

    @Test
    void 비밀번호_변경_현재_비밀번호가_일치하지_않으면_예외가_발생한다() {
        // given
        Long currentUserId = 1L;

        UpdateUserPasswordRequest request =
                new UpdateUserPasswordRequest(
                        "WrongPassword1!",
                        "NewPassword1!"
                );

        RuntimeException exception =
                new RuntimeException("current password mismatch");

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        willThrow(exception)
                .given(userValidator)
                .validatePassword(
                        request.currentPassword(),
                        user
                );

        // when & then
        assertThatThrownBy(() ->
                userService.updateMyPassword(request, currentUserId)
        )
                .isSameAs(exception);

        then(userValidator).should(never())
                .validateNewPassword(any(), any());

        then(passwordEncoder).should(never())
                .encode(any());
    }

    @Test
    void 비밀번호_변경_새_비밀번호가_기존_비밀번호와_같으면_예외가_발생한다() {
        // given
        Long currentUserId = 1L;

        UpdateUserPasswordRequest request =
                new UpdateUserPasswordRequest(
                        "Password1!",
                        "Password1!"
                );

        RuntimeException exception =
                new RuntimeException("same password");

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        willThrow(exception)
                .given(userValidator)
                .validateNewPassword(
                        request.newPassword(),
                        user
                );

        // when & then
        assertThatThrownBy(() ->
                userService.updateMyPassword(request, currentUserId)
        )
                .isSameAs(exception);

        then(userValidator).should()
                .validatePassword(
                        request.currentPassword(),
                        user
                );

        then(passwordEncoder).should(never())
                .encode(any());
    }

    @Test
    void 비밀번호_변경_성공() {
        // given
        Long currentUserId = 1L;

        UpdateUserPasswordRequest request =
                new UpdateUserPasswordRequest(
                        "Password1!",
                        "NewPassword1!"
                );

        String encodedPassword = "encoded-new-password";

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        given(passwordEncoder.encode(request.newPassword()))
                .willReturn(encodedPassword);

        // when
        userService.updateMyPassword(request, currentUserId);

        // then
        then(userValidator).should()
                .validateActiveUser(currentUserId);

        then(userValidator).should()
                .validatePassword(
                        request.currentPassword(),
                        user
                );

        then(userValidator).should()
                .validateNewPassword(
                        request.newPassword(),
                        user
                );

        then(passwordEncoder).should()
                .encode(request.newPassword());

        assertThat(user.getPassword())
                .isEqualTo(encodedPassword);
    }

    @Test
    void 회원_탈퇴_성공하면_사용자를_저장하고_탈퇴_Outbox_이벤트를_발행한다() {
        // given
        Long currentUserId = 1L;

        WithdrawUserRequest request =
                new WithdrawUserRequest("Password1!");

        String encodedPassword = "encoded-withdraw-password";
        String originalLoginId = user.getLoginId();

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        given(passwordEncoder.encode(request.password()))
                .willReturn(encodedPassword);

        // when
        userService.withdraw(request, currentUserId);

        // then
        then(userValidator).should()
                .validateActiveUser(currentUserId);

        then(userValidator).should()
                .validatePassword(
                        request.password(),
                        user
                );

        then(passwordEncoder).should()
                .encode(request.password());

        then(userRepository).should()
                .saveAndFlush(user);

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<UserWithdrawEvent> eventCaptor =
                ArgumentCaptor.forClass(UserWithdrawEvent.class);

        then(outboxService).should()
                .save(
                        eventIdCaptor.capture(),
                        org.mockito.ArgumentMatchers.eq(
                                OutboxAggregateType.USER
                        ),
                        org.mockito.ArgumentMatchers.eq(
                                String.valueOf(user.getId())
                        ),
                        org.mockito.ArgumentMatchers.eq(
                                OutboxEventType.USER_WITHDRAW
                        ),
                        eventCaptor.capture()
                );

        String eventId = eventIdCaptor.getValue();
        UserWithdrawEvent event = eventCaptor.getValue();

        assertThat(eventId)
                .isNotBlank();

        assertThat(event.eventId())
                .isEqualTo(eventId);

        assertThat(event.userId())
                .isEqualTo(user.getId());

        assertThat(event.loginId())
                .isEqualTo(originalLoginId);

        assertThat(user.getUserStatus())
                .isEqualTo(com.example.schedulebook.domain.user.enums.UserStatus.WITHDRAW);

        assertThat(user.getDeletedAt())
                .isNotNull();

        assertThat(user.getLoginId())
                .isNotEqualTo(originalLoginId);

        assertThat(user.getNickname())
                .contains(CommonConst.WITHDRAW_USER);
    }

    @Test
    void 회원_탈퇴_비밀번호가_일치하지_않으면_탈퇴하지_않고_Outbox_이벤트도_발행하지_않는다() {
        // given
        Long currentUserId = 1L;

        WithdrawUserRequest request =
                new WithdrawUserRequest("WrongPassword1!");

        RuntimeException exception =
                new RuntimeException("password mismatch");

        given(userValidator.validateActiveUser(currentUserId))
                .willReturn(user);

        willThrow(exception)
                .given(userValidator)
                .validatePassword(
                        request.password(),
                        user
                );

        // when & then
        assertThatThrownBy(() ->
                userService.withdraw(request, currentUserId)
        )
                .isSameAs(exception);

        then(userRepository).should(never())
                .saveAndFlush(any());

        then(outboxService).should(never())
                .save(
                        anyString(),
                        any(),
                        anyString(),
                        any(),
                        any()
                );
    }

    @Test
    void 회원_탈퇴_비활성_사용자이면_탈퇴하지_않는다() {
        // given
        Long currentUserId = 1L;

        WithdrawUserRequest request =
                new WithdrawUserRequest("Password1!");

        RuntimeException exception =
                new RuntimeException("inactive user");

        given(userValidator.validateActiveUser(currentUserId))
                .willThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                userService.withdraw(request, currentUserId)
        )
                .isSameAs(exception);

        then(userValidator).should(never())
                .validatePassword(any(), any());

        then(userRepository).should(never())
                .saveAndFlush(any());

        then(outboxService).should(never())
                .save(
                        anyString(),
                        any(),
                        anyString(),
                        any(),
                        any()
                );
    }
}