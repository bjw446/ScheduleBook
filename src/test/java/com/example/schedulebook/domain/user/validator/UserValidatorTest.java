package com.example.schedulebook.domain.user.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserRole;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserValidator userValidator;

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
    }

    @Test
    void 활성_사용자_조회_성공() {
        // given
        Long userId = 1L;

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when
        User result = userValidator.validateActiveUser(userId);

        // then
        assertThat(result).isSameAs(user);
        assertThat(result.getUserStatus())
                .isEqualTo(UserStatus.ACTIVE);

        then(userRepository).should()
                .findById(userId);
    }

    @Test
    void 활성_사용자_조회_대상이_없으면_예외가_발생한다() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateActiveUser(userId)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.USER_NOT_FOUND);
    }

    @Test
    void 활성_사용자_조회_대상이_비활성_상태이면_예외가_발생한다() {
        // given
        Long userId = 1L;

        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.WITHDRAW
        );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateActiveUser(userId)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.USER_NOT_ACTIVE);
    }

    @Test
    void 프로필_수정_중복_검증_모든_값이_변경되지_않았다면_정상_통과한다() {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                user.getNickname(),
                user.getEmail(),
                user.getPhoneNumber()
        );

        // when & then
        assertThatCode(() ->
                userValidator.validateDuplicate(request, user)
        )
                .doesNotThrowAnyException();

        then(userRepository).should(never())
                .existsByNickname(anyString());

        then(userRepository).should(never())
                .existsByEmail(anyString());

        then(userRepository).should(never())
                .existsByPhoneNumber(anyString());
    }

    @Test
    void 프로필_수정_닉네임이_중복되면_예외가_발생한다() {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                "중복닉네임",
                user.getEmail(),
                user.getPhoneNumber()
        );

        given(userRepository.existsByNickname(request.nickname()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateDuplicate(request, user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.NICKNAME_ALREADY_EXISTS);

        then(userRepository).should()
                .existsByNickname(request.nickname());

        then(userRepository).should(never())
                .existsByEmail(anyString());

        then(userRepository).should(never())
                .existsByPhoneNumber(anyString());
    }

    @Test
    void 프로필_수정_이메일이_중복되면_예외가_발생한다() {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                user.getNickname(),
                "duplicate@example.com",
                user.getPhoneNumber()
        );

        given(userRepository.existsByEmail(request.email()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateDuplicate(request, user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.EMAIL_ALREADY_EXISTS);

        then(userRepository).should()
                .existsByEmail(request.email());

        then(userRepository).should(never())
                .existsByPhoneNumber(anyString());
    }

    @Test
    void 프로필_수정_전화번호가_중복되면_예외가_발생한다() {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                user.getNickname(),
                user.getEmail(),
                "010-9999-9999"
        );

        given(userRepository.existsByPhoneNumber(request.phoneNumber()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateDuplicate(request, user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.PHONE_NUMBER_ALREADY_EXISTS);
    }

    @Test
    void 현재_비밀번호가_일치하면_정상_통과한다() {
        // given
        String currentPassword = "Password1!";

        given(passwordEncoder.matches(
                currentPassword,
                user.getPassword()
        ))
                .willReturn(true);

        // when & then
        assertThatCode(() ->
                userValidator.validatePassword(currentPassword, user)
        )
                .doesNotThrowAnyException();

        then(passwordEncoder).should()
                .matches(currentPassword, user.getPassword());
    }

    @Test
    void 현재_비밀번호가_일치하지_않으면_예외가_발생한다() {
        // given
        String currentPassword = "WrongPassword1!";

        given(passwordEncoder.matches(
                currentPassword,
                user.getPassword()
        ))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validatePassword(currentPassword, user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.PASSWORD_NOT_MATCH);
    }

    @Test
    void 새_비밀번호가_기존_비밀번호와_다르면_정상_통과한다() {
        // given
        String newPassword = "NewPassword1!";

        given(passwordEncoder.matches(
                newPassword,
                user.getPassword()
        ))
                .willReturn(false);

        // when & then
        assertThatCode(() ->
                userValidator.validateNewPassword(newPassword, user)
        )
                .doesNotThrowAnyException();
    }

    @Test
    void 새_비밀번호가_기존_비밀번호와_같으면_예외가_발생한다() {
        // given
        String newPassword = "Password1!";

        given(passwordEncoder.matches(
                newPassword,
                user.getPassword()
        ))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateNewPassword(newPassword, user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.PASSWORD_SAME_AS_OLD);
    }

    @Test
    void 자기_자신에게_공유하려하면_예외가_발생한다() {
        // given
        Long currentUserId = 1L;
        Long friendId = 1L;

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateShareMyself(
                        currentUserId,
                        friendId
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.CANNOT_SHARE_MYSELF);
    }

    @Test
    void 다른_사용자에게_공유하면_정상_통과한다() {
        // given
        Long currentUserId = 1L;
        Long friendId = 2L;

        // when & then
        assertThatCode(() ->
                userValidator.validateShareMyself(
                        currentUserId,
                        friendId
                )
        )
                .doesNotThrowAnyException();
    }

    @Test
    void 활성_슈퍼_관리자_조회_성공() {
        // given
        Long adminId = 1L;

        ReflectionTestUtils.setField(
                user,
                "userRole",
                UserRole.SUPER_ADMIN
        );

        given(userRepository.findById(adminId))
                .willReturn(Optional.of(user));

        // when
        User result = userValidator.validateActiveAdmin(adminId);

        // then
        assertThat(result).isSameAs(user);
        assertThat(result.getUserRole())
                .isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(result.getUserStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void 관리자_조회_대상이_없으면_예외가_발생한다() {
        // given
        Long adminId = 999L;

        given(userRepository.findById(adminId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateActiveAdmin(adminId)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.ADMIN_NOT_FOUND);
    }

    @Test
    void 관리자_조회_대상이_슈퍼_관리자가_아니면_예외가_발생한다() {
        // given
        Long adminId = 1L;

        ReflectionTestUtils.setField(
                user,
                "userRole",
                UserRole.USER
        );

        given(userRepository.findById(adminId))
                .willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateActiveAdmin(adminId)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.ADMIN_NOT_FOUND);

        then(userRepository).should()
                .findById(adminId);
    }

    @Test
    void 관리자_조회_대상이_비활성_상태이면_예외가_발생한다() {
        // given
        Long adminId = 1L;

        ReflectionTestUtils.setField(
                user,
                "userRole",
                UserRole.SUPER_ADMIN
        );

        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.WITHDRAW
        );

        given(userRepository.findById(adminId))
                .willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateActiveAdmin(adminId)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.ADMIN_NOT_ACTIVE);
    }

    @Test
    void 회원가입_중복_검증에서_loginId가_중복되면_예외가_발생한다() {
        // given
        String loginId = "duplicate";
        String email = "new@example.com";
        String nickname = "새닉네임";
        String phoneNumber = "010-1111-2222";

        given(userRepository.existsByLoginId(loginId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.LOGIN_ID_ALREADY_EXISTS);

        then(userRepository).should()
                .existsByLoginId(loginId);

        then(userRepository).should(never())
                .existsByEmail(anyString());
    }

    @Test
    void 회원가입_중복_검증에서_email이_중복되면_예외가_발생한다() {
        // given
        String loginId = "newuser";
        String email = "duplicate@example.com";
        String nickname = "새닉네임";
        String phoneNumber = "010-1111-2222";

        given(userRepository.existsByLoginId(loginId))
                .willReturn(false);

        given(userRepository.existsByEmail(email))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.EMAIL_ALREADY_EXISTS);

        then(userRepository).should()
                .existsByLoginId(loginId);

        then(userRepository).should()
                .existsByEmail(email);

        then(userRepository).should(never())
                .existsByNickname(anyString());
    }

    @Test
    void 회원가입_중복_검증에서_nickname이_중복되면_예외가_발생한다() {
        // given
        String loginId = "newuser";
        String email = "new@example.com";
        String nickname = "중복닉네임";
        String phoneNumber = "010-1111-2222";

        given(userRepository.existsByLoginId(loginId))
                .willReturn(false);

        given(userRepository.existsByEmail(email))
                .willReturn(false);

        given(userRepository.existsByNickname(nickname))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.NICKNAME_ALREADY_EXISTS);

        then(userRepository).should()
                .existsByNickname(nickname);

        then(userRepository).should(never())
                .existsByPhoneNumber(anyString());
    }

    @Test
    void 회원가입_중복_검증에서_phoneNumber가_중복되면_예외가_발생한다() {
        // given
        String loginId = "newuser";
        String email = "new@example.com";
        String nickname = "새닉네임";
        String phoneNumber = "010-9999-9999";

        given(userRepository.existsByLoginId(loginId))
                .willReturn(false);

        given(userRepository.existsByEmail(email))
                .willReturn(false);

        given(userRepository.existsByNickname(nickname))
                .willReturn(false);

        given(userRepository.existsByPhoneNumber(phoneNumber))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.PHONE_NUMBER_ALREADY_EXISTS);
    }

    @Test
    void 로그인_사용자_상태가_ACTIVE이면_정상_통과한다() {
        // given
        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.ACTIVE
        );

        // when & then
        assertThatCode(() ->
                userValidator.validateLoginUserStatus(user)
        )
                .doesNotThrowAnyException();
    }

    @Test
    void 로그인_사용자_상태가_ACTIVE가_아니면_로그인_실패_예외가_발생한다() {
        // given
        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.WITHDRAW
        );

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateLoginUserStatus(user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.LOGIN_FAILED);
    }

    @Test
    void 사용자_상태가_ACTIVE이면_정상_통과한다() {
        // given
        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.ACTIVE
        );

        // when & then
        assertThatCode(() ->
                userValidator.validateUserStatus(user)
        )
                .doesNotThrowAnyException();
    }

    @Test
    void 사용자_상태가_ACTIVE가_아니면_예외가_발생한다() {
        // given
        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.WITHDRAW
        );

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateUserStatus(user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.USER_NOT_ACTIVE);
    }

    @Test
    void 관리자_상태가_ACTIVE이면_정상_통과한다() {
        // given
        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.ACTIVE
        );

        // when & then
        assertThatCode(() ->
                userValidator.validateAdminStatus(user)
        )
                .doesNotThrowAnyException();
    }

    @Test
    void 관리자_상태가_ACTIVE가_아니면_예외가_발생한다() {
        // given
        ReflectionTestUtils.setField(
                user,
                "userStatus",
                UserStatus.WITHDRAW
        );

        // when & then
        assertThatThrownBy(() ->
                userValidator.validateAdminStatus(user)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorEnum()
                )
                .isEqualTo(ErrorEnum.ADMIN_NOT_ACTIVE);
    }
}