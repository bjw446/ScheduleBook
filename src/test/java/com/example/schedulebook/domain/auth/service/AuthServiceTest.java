package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.exception.SessionLimitException;
import com.example.schedulebook.common.security.JwtProvider;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.example.schedulebook.domain.auth.dto.request.RefreshRequest;
import com.example.schedulebook.domain.auth.dto.request.SignupRequest;
import com.example.schedulebook.domain.auth.dto.response.LoginResponse;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.dto.response.SignupResponse;
import com.example.schedulebook.domain.auth.dto.response.SessionLimitResult;
import com.example.schedulebook.domain.auth.dto.token.LoginToken;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.AuditEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import com.example.schedulebook.domain.user.validator.UserValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator userValidator;

    @Mock
    private LoginFailureService loginFailureService;

    @Mock
    private LoginSuccessService loginSuccessService;

    @Mock
    private SessionService sessionService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private SessionLimitService sessionLimitService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AuditEventOutboxService auditEventOutboxService;

    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private AuthService authService;

    private User user;

    private final Long userId = 1L;
    private final String loginId = "testuser";
    private final String password = "Password1!";
    private final String encodedPassword = "encoded-password";
    private final String nickname = "테스트";
    private final String email = "test@test.com";
    private final String phoneNumber = "010-1234-5678";

    private final String sessionId = "session-id";
    private final String accessToken = "access-token";
    private final String refreshToken = "refresh-token";

    @BeforeEach
    void setUp() {
        user = User.create(
                loginId,
                encodedPassword,
                nickname,
                email,
                phoneNumber
        );

        ReflectionTestUtils.setField(user, "id", userId);
    }

    // ============================================================
    // signup
    // ============================================================

    @Test
    void givenValidSignupRequest_whenSignup_thenUser를_저장하고_응답을_반환한다() {
        // given
        SignupRequest request = new SignupRequest(
                loginId,
                password,
                nickname,
                email,
                phoneNumber
        );

        when(passwordEncoder.encode(password))
                .thenReturn(encodedPassword);

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response).isNotNull();

        verify(userValidator).validateDuplicateUser(
                loginId,
                email,
                nickname,
                phoneNumber
        );

        verify(passwordEncoder).encode(password);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getLoginId())
                .isEqualTo(loginId);
        assertThat(savedUser.getPassword())
                .isEqualTo(encodedPassword);
        assertThat(savedUser.getNickname())
                .isEqualTo(nickname);
        assertThat(savedUser.getEmail())
                .isEqualTo(email);
        assertThat(savedUser.getPhoneNumber())
                .isEqualTo(phoneNumber);
    }

    @Test
    void given로그인ID가_중복된_회원_whenSignup_then예외를_전파하고_저장하지_않는다() {
        // given
        SignupRequest request = new SignupRequest(
                loginId,
                password,
                nickname,
                email,
                phoneNumber
        );

        doThrow(new BaseException(ErrorEnum.LOGIN_ID_ALREADY_EXISTS))
                .when(userValidator)
                .validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                );

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.LOGIN_ID_ALREADY_EXISTS);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void givenEmail이_중복된_회원_whenSignup_then예외를_전파하고_저장하지_않는다() {
        // given
        SignupRequest request = new SignupRequest(
                loginId,
                password,
                nickname,
                email,
                phoneNumber
        );

        doThrow(new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS))
                .when(userValidator)
                .validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                );

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.EMAIL_ALREADY_EXISTS);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void givenNickname이_중복된_회원_whenSignup_then예외를_전파하고_저장하지_않는다() {
        // given
        SignupRequest request = new SignupRequest(
                loginId,
                password,
                nickname,
                email,
                phoneNumber
        );

        doThrow(new BaseException(ErrorEnum.NICKNAME_ALREADY_EXISTS))
                .when(userValidator)
                .validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                );

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.NICKNAME_ALREADY_EXISTS);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void given전화번호가_중복된_회원_whenSignup_then예외를_전파하고_저장하지_않는다() {
        // given
        SignupRequest request = new SignupRequest(
                loginId,
                password,
                nickname,
                email,
                phoneNumber
        );

        doThrow(new BaseException(ErrorEnum.PHONE_NUMBER_ALREADY_EXISTS))
                .when(userValidator)
                .validateDuplicateUser(
                        loginId,
                        email,
                        nickname,
                        phoneNumber
                );

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.PHONE_NUMBER_ALREADY_EXISTS);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // login - common
    // ============================================================

    @Test
    void given잠긴_로그인_계정_whenLogin_then로그인을_진행하지_않는다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        doThrow(new BaseException(ErrorEnum.ACCOUNT_LOCKED))
                .when(loginFailureService)
                .validateNotLocked(loginId);

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.ACCOUNT_LOCKED);

        verify(userRepository, never()).findByLoginId(anyString());
        verifyNoInteractions(passwordEncoder, sessionService);
    }

    @Test
    void given존재하지_않는_사용자_whenLogin_thenLOGIN_FAILED를_던진다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.LOGIN_FAILED);

        verify(userValidator, never())
                .validateLoginUserStatus(any());

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());
    }

    @Test
    void given비활성_사용자_whenLogin_thenLOGIN_FAILED를_던진다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        doThrow(new BaseException(ErrorEnum.LOGIN_FAILED))
                .when(userValidator)
                .validateLoginUserStatus(user);

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.LOGIN_FAILED);

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(sessionService, never())
                .generateSessionId();
    }

    @Test
    void given잘못된_비밀번호_whenLogin_then실패_로그인을_기록하고_LOGIN_FAILED를_던진다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(servletRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(servletRequest.getHeader("User-Agent"))
                .thenReturn("JUnit");

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.LOGIN_FAILED);

        verify(loginFailureService).handleFailure(
                anyString(),
                eq(loginId),
                eq("127.0.0.1"),
                eq("JUnit")
        );

        verify(sessionLimitService, never())
                .reserveSession(anyLong(), any(), anyString());

        verify(sessionService, never())
                .createSession(
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void given정상_비밀번호와_세션_whenLogin_thenAccessToken과RefreshToken을_반환한다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        LoginToken token = new LoginToken(
                userId,
                sessionId,
                accessToken,
                refreshToken
        );

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(servletRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(servletRequest.getHeader("User-Agent"))
                .thenReturn("JUnit");

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.reserveSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(sessionId)
        )).thenReturn(SessionLimitResult.available());

        when(sessionService.createSession(
                eq(userId),
                eq(sessionId),
                eq("127.0.0.1"),
                eq("JUnit"),
                eq(user.getUserRole())
        )).thenReturn(token);

        // when
        LoginResponse response =
                authService.login(request, servletRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken())
                .isEqualTo(accessToken);
        assertThat(response.refreshToken())
                .isEqualTo(refreshToken);

        verify(sessionLimitService).reserveSession(
                userId,
                user.getUserRole(),
                sessionId
        );

        verify(loginSuccessService)
                .loginSuccess(user, "127.0.0.1", "JUnit");

        verify(sessionService).createSession(
                userId,
                sessionId,
                "127.0.0.1",
                "JUnit",
                user.getUserRole()
        );
    }

    @Test
    void given세션_제한을_초과한_상태_whenLogin_thenSessionLimitException을_던진다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.reserveSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(sessionId)
        )).thenReturn(
                SessionLimitResult.exceeded(List.of())
        );

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(SessionLimitException.class);

        verify(loginSuccessService, never())
                .loginSuccess(any(), anyString(), anyString());

        verify(sessionService, never())
                .createSession(
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any()
                );

        verify(sessionService, never())
                .removeSession(anyLong(), anyString());
    }

    @Test
    void given로그인_후처리_중_예외_whenLogin_then예약된_세션을_제거한다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.reserveSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(sessionId)
        )).thenReturn(SessionLimitResult.available());

        doThrow(new BaseException(ErrorEnum.LOGIN_CONFLICT))
                .when(loginSuccessService)
                .loginSuccess(
                        eq(user),
                        anyString(),
                        anyString()
                );

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.LOGIN_CONFLICT);

        verify(sessionService)
                .removeSession(userId, sessionId);
    }

    @Test
    void given로그인_중_OptimisticLock_충돌_whenLogin_thenLOGIN_CONFLICT로_변환하고_세션을_정리한다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.reserveSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(sessionId)
        )).thenReturn(SessionLimitResult.available());

        doThrow(new ObjectOptimisticLockingFailureException(
                User.class,
                userId
        ))
                .when(loginSuccessService)
                .loginSuccess(
                        eq(user),
                        eq("UNKNOWN"),
                        eq("UNKNOWN")
                );

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.LOGIN_CONFLICT);

        verify(sessionService)
                .removeSession(userId, sessionId);
    }

    // ============================================================
    // login - replace session
    // ============================================================

    @Test
    void given교체할_세션이_있는_정상_로그인_whenLogin_then새_세션을_생성하고_CleanupOutbox를_저장한다() {
        // given
        String oldSessionId = "old-session-id";

        LoginRequest request =
                new LoginRequest(loginId, password, oldSessionId);

        LoginToken token = new LoginToken(
                userId,
                sessionId,
                accessToken,
                refreshToken
        );

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.replaceSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(oldSessionId),
                eq(sessionId),
                anyString()
        )).thenReturn(SessionLimitResult.available());

        when(sessionService.createSession(
                eq(userId),
                eq(sessionId),
                eq("UNKNOWN"),
                eq("UNKNOWN"),
                eq(user.getUserRole())
        )).thenReturn(token);

        // when
        LoginResponse response =
                authService.login(request, servletRequest);

        // then
        assertThat(response.accessToken())
                .isEqualTo(accessToken);

        assertThat(response.refreshToken())
                .isEqualTo(refreshToken);

        verify(sessionLimitService).replaceSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(oldSessionId),
                eq(sessionId),
                anyString()
        );

        verify(loginSuccessService)
                .loginSuccess(
                        user,
                        "UNKNOWN",
                        "UNKNOWN"
                );

        verify(sessionService).createSession(
                userId,
                sessionId,
                "UNKNOWN",
                "UNKNOWN",
                user.getUserRole()
        );

        verify(outboxService, times(2))
                .save(
                        anyString(),
                        any(OutboxAggregateType.class),
                        anyString(),
                        any(OutboxEventType.class),
                        any()
                );
    }

    @Test
    void given교체_세션_제한을_초과한_상태_whenLogin_thenSessionLimitException을_던진다() {
        // given
        String oldSessionId = "old-session-id";

        LoginRequest request =
                new LoginRequest(loginId, password, oldSessionId);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.replaceSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(oldSessionId),
                eq(sessionId),
                anyString()
        )).thenReturn(
                SessionLimitResult.exceeded(List.of())
        );

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(SessionLimitException.class);

        verify(sessionService, never())
                .createSession(
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any()
                );

        verify(sessionService, never())
                .rollbackReplacedSession(
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void given교체_로그인_후처리_중_예외_whenLogin_then교체_세션을_롤백한다() {
        // given
        String oldSessionId = "old-session-id";

        LoginRequest request =
                new LoginRequest(loginId, password, oldSessionId);

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.replaceSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(oldSessionId),
                eq(sessionId),
                anyString()
        )).thenReturn(SessionLimitResult.available());

        doThrow(new BaseException(ErrorEnum.LOGIN_CONFLICT))
                .when(loginSuccessService)
                .loginSuccess(
                        eq(user),
                        anyString(),
                        anyString()
                );

        // when & then
        assertThatThrownBy(() ->
                authService.login(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.LOGIN_CONFLICT);

        verify(sessionService).rollbackReplacedSession(
                eq(userId),
                eq(oldSessionId),
                eq(sessionId),
                anyString()
        );
    }

    // ============================================================
    // logout
    // ============================================================

    @Test
    void given유효한_accessToken_whenLogout_thenSessionService에_로그아웃을_위임하고_AuditOutbox를_저장한다() {
        // given
        LoginToken token = new LoginToken(
                userId,
                sessionId,
                accessToken,
                null
        );

        when(servletRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(servletRequest.getHeader("User-Agent"))
                .thenReturn("JUnit");

        when(sessionService.logout(accessToken))
                .thenReturn(token);

        // when
        authService.logout(accessToken, servletRequest);

        // then
        verify(sessionService)
                .logout(accessToken);

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(outboxService).save(
                anyString(),
                eq(OutboxAggregateType.USER),
                eq(String.valueOf(userId)),
                eq(OutboxEventType.AUDIT_EVENT),
                captor.capture()
        );

        AuditEvent event = captor.getValue();

        assertThat(event.userId())
                .isEqualTo(userId);

        assertThat(event.eventType())
                .isEqualTo(AuditEventType.LOGOUT);

        assertThat(event.ip())
                .isEqualTo("127.0.0.1");

        assertThat(event.userAgent())
                .isEqualTo("JUnit");
    }

    @Test
    void givenSessionService_로그아웃_실패_whenLogout_thenAuditOutbox를_저장하지_않는다() {
        // given
        doThrow(new BaseException(ErrorEnum.TOKEN_INVALID))
                .when(sessionService)
                .logout(accessToken);

        // when & then
        assertThatThrownBy(() ->
                authService.logout(accessToken, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.TOKEN_INVALID);

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(),
                        anyString(),
                        any(),
                        any()
                );
    }

    // ============================================================
    // refresh
    // ============================================================

    @Test
    void given유효한_refreshToken_whenRefresh_then새로운_토큰을_반환한다() {
        // given
        RefreshRequest request =
                new RefreshRequest(refreshToken);

        LoginToken token = new LoginToken(
                userId,
                sessionId,
                accessToken,
                refreshToken
        );

        when(sessionService.refresh(refreshToken))
                .thenReturn(token);

        when(userValidator.validateActiveUser(userId))
                .thenReturn(user);

        // when
        LoginResponse response =
                authService.refresh(request, servletRequest);

        // then
        assertThat(response).isNotNull();

        assertThat(response.accessToken())
                .isEqualTo(accessToken);

        assertThat(response.refreshToken())
                .isEqualTo(refreshToken);

        verify(sessionService)
                .refresh(refreshToken);

        verify(userValidator)
                .validateActiveUser(userId);
    }

    @Test
    void givenRefreshToken이_만료되었거나_유효하지_않을때_whenRefresh_then예외를_전파한다() {
        // given
        RefreshRequest request =
                new RefreshRequest(refreshToken);

        doThrow(new BaseException(ErrorEnum.REFRESH_TOKEN_INVALID))
                .when(sessionService)
                .refresh(refreshToken);

        // when & then
        assertThatThrownBy(() ->
                authService.refresh(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.REFRESH_TOKEN_INVALID);

        verify(userValidator, never())
                .validateActiveUser(anyLong());

        verify(auditEventOutboxService, never())
                .saveReplayEvent(
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void givenRefreshTokenReplay가_탐지되었을때_whenRefresh_then사용자_검증과_감사이벤트를_처리하고_REFRESH_TOKEN_INVALID를_던진다() {
        // given
        RefreshRequest request =
                new RefreshRequest(refreshToken);

        when(sessionService.refresh(refreshToken))
                .thenThrow(
                        new BaseException(ErrorEnum.REFRESH_TOKEN_REPLAY)
                );

        when(jwtProvider.extractUserId(refreshToken))
                .thenReturn(userId);

        when(userValidator.validateActiveUser(userId))
                .thenReturn(user);

        // when & then
        assertThatThrownBy(() ->
                authService.refresh(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.REFRESH_TOKEN_INVALID);

        verify(jwtProvider)
                .extractUserId(refreshToken);

        verify(userValidator)
                .validateActiveUser(userId);

        verify(auditEventOutboxService)
                .saveReplayEvent(
                        eq(userId),
                        eq(loginId),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void givenReplayToken의_userId를_추출할수_없을때_whenRefresh_then예외를_전파한다() {
        // given
        RefreshRequest request =
                new RefreshRequest(refreshToken);

        when(sessionService.refresh(refreshToken))
                .thenThrow(
                        new BaseException(ErrorEnum.REFRESH_TOKEN_REPLAY)
                );

        when(jwtProvider.extractUserId(refreshToken))
                .thenThrow(
                        new BaseException(ErrorEnum.TOKEN_INVALID)
                );

        // when & then
        assertThatThrownBy(() ->
                authService.refresh(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.TOKEN_INVALID);

        verify(auditEventOutboxService, never())
                .saveReplayEvent(
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void givenReplayToken의_사용자가_비활성일때_whenRefresh_then감사이벤트를_저장하지_않는다() {
        // given
        RefreshRequest request =
                new RefreshRequest(refreshToken);

        when(sessionService.refresh(refreshToken))
                .thenThrow(
                        new BaseException(ErrorEnum.REFRESH_TOKEN_REPLAY)
                );

        when(jwtProvider.extractUserId(refreshToken))
                .thenReturn(userId);

        doThrow(new BaseException(ErrorEnum.USER_NOT_ACTIVE))
                .when(userValidator)
                .validateActiveUser(userId);

        // when & then
        assertThatThrownBy(() ->
                authService.refresh(request, servletRequest)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.USER_NOT_ACTIVE);

        verify(auditEventOutboxService, never())
                .saveReplayEvent(
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    // ============================================================
    // findMySessions
    // ============================================================

    @Test
    void given활성_사용자_whenFindMySessions_then세션목록을_반환한다() {
        // given
        List<SessionInfoResponse> sessions =
                List.of();

        when(userValidator.validateActiveUser(userId))
                .thenReturn(user);

        when(sessionService.findSessions(userId))
                .thenReturn(sessions);

        // when
        List<SessionInfoResponse> result =
                authService.findMySessions(userId);

        // then
        assertThat(result)
                .isSameAs(sessions);

        verify(userValidator)
                .validateActiveUser(userId);

        verify(sessionService)
                .findSessions(userId);
    }

    @Test
    void given비활성_사용자_whenFindMySessions_then세션을_조회하지_않는다() {
        // given
        doThrow(new BaseException(ErrorEnum.USER_NOT_FOUND))
                .when(userValidator)
                .validateActiveUser(userId);

        // when & then
        assertThatThrownBy(() ->
                authService.findMySessions(userId)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.USER_NOT_FOUND);

        verify(sessionService, never())
                .findSessions(anyLong());
    }

    // ============================================================
    // logoutSession
    // ============================================================

    @Test
    void given활성_사용자와_sessionId_whenLogoutSession_then세션을_로그아웃하고_AuditOutbox를_저장한다() {
        // given
        when(servletRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(servletRequest.getHeader("User-Agent"))
                .thenReturn("JUnit");

        when(userValidator.validateActiveUser(userId))
                .thenReturn(user);

        // when
        authService.logoutSession(
                userId,
                sessionId,
                servletRequest
        );

        // then
        verify(userValidator)
                .validateActiveUser(userId);

        verify(sessionService)
                .logoutSession(userId, sessionId);

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(outboxService).save(
                anyString(),
                eq(OutboxAggregateType.USER),
                eq(String.valueOf(userId)),
                eq(OutboxEventType.AUDIT_EVENT),
                captor.capture()
        );

        AuditEvent event = captor.getValue();

        assertThat(event.userId())
                .isEqualTo(userId);

        assertThat(event.eventType())
                .isEqualTo(AuditEventType.SESSION_LOGOUT);

        assertThat(event.ip())
                .isEqualTo("127.0.0.1");

        assertThat(event.userAgent())
                .isEqualTo("JUnit");
    }

    @Test
    void given사용자_검증에_실패했을때_whenLogoutSession_then세션_로그아웃과_AuditOutbox를_실행하지_않는다() {
        // given
        doThrow(new BaseException(ErrorEnum.USER_NOT_FOUND))
                .when(userValidator)
                .validateActiveUser(userId);

        // when & then
        assertThatThrownBy(() ->
                authService.logoutSession(
                        userId,
                        sessionId,
                        servletRequest
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.USER_NOT_FOUND);

        verify(sessionService, never())
                .logoutSession(anyLong(), anyString());

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(),
                        anyString(),
                        any(),
                        any()
                );
    }

    @Test
    void givenSessionService_세션_로그아웃에_실패했을때_whenLogoutSession_thenAuditOutbox를_저장하지_않는다() {
        // given
        when(userValidator.validateActiveUser(userId))
                .thenReturn(user);

        doThrow(new BaseException(ErrorEnum.SESSION_NOT_FOUND))
                .when(sessionService)
                .logoutSession(userId, sessionId);

        // when & then
        assertThatThrownBy(() ->
                authService.logoutSession(
                        userId,
                        sessionId,
                        servletRequest
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SESSION_NOT_FOUND);

        verify(outboxService, never())
                .save(
                        anyString(),
                        any(),
                        anyString(),
                        any(),
                        any()
                );
    }

    // ============================================================
    // IP / User-Agent fallback
    // ============================================================

    @Test
    void givenIP와UserAgent가_없을때_whenLogin_thenUNKNOWN을_사용한다() {
        // given
        LoginRequest request =
                new LoginRequest(loginId, password, null);

        LoginToken token = new LoginToken(
                userId,
                sessionId,
                accessToken,
                refreshToken
        );

        when(userRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(user));

        when(servletRequest.getRemoteAddr())
                .thenReturn(null);

        when(servletRequest.getHeader("User-Agent"))
                .thenReturn(null);

        when(sessionService.generateSessionId())
                .thenReturn(sessionId);

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(sessionLimitService.reserveSession(
                eq(userId),
                eq(user.getUserRole()),
                eq(sessionId)
        )).thenReturn(SessionLimitResult.available());

        when(sessionService.createSession(
                eq(userId),
                eq(sessionId),
                eq("UNKNOWN"),
                eq("UNKNOWN"),
                eq(user.getUserRole())
        )).thenReturn(token);

        // when
        authService.login(request, servletRequest);

        // then
        verify(loginSuccessService)
                .loginSuccess(
                        user,
                        "UNKNOWN",
                        "UNKNOWN"
                );

        verify(sessionService)
                .createSession(
                        userId,
                        sessionId,
                        "UNKNOWN",
                        "UNKNOWN",
                        user.getUserRole()
                );
    }
}