package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.exception.SessionLimitException;
import com.example.schedulebook.common.security.JwtProvider;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.example.schedulebook.domain.auth.dto.request.RefreshRequest;
import com.example.schedulebook.domain.auth.dto.request.SignupRequest;
import com.example.schedulebook.domain.auth.dto.response.*;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final LoginFailureService loginFailureService;
    private final LoginSuccessService loginSuccessService;
    private final SessionService sessionService;
    private final JwtProvider jwtProvider;
    private final SessionLimitService sessionLimitService;
    private final OutboxService outboxService;

    public SignupResponse signup(SignupRequest request) {
        userValidator.validateDuplicateUser(
                request.loginId(),
                request.email(),
                request.nickname(),
                request.phoneNumber()
        );

        User user = User.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.email(),
                request.phoneNumber()
        );

        User savedUser = userRepository.save(user);

        return SignupResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        loginFailureService.validateNotLocked(request.loginId());

        User user = userRepository.findByLoginId(request.loginId()).orElseThrow(
                () -> new BaseException(ErrorEnum.LOGIN_FAILED)
        );

        userValidator.validateLoginUserStatus(user);

        String ip = getUserIp(servletRequest);

        String userAgent = getUserAgent(servletRequest);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginFailureService.handleFailure(request.loginId(), ip, userAgent);

            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        if (request.replaceSessionId() != null && !request.replaceSessionId().isBlank()) {
            SessionLimitResult sessionLimitResult = sessionLimitService.validateSessionLimitExcluding(
                    user.getId(),
                    user.getUserRole(),
                    request.replaceSessionId()
            );

            if (sessionLimitResult.exceeded()) {
                throw new SessionLimitException(ErrorEnum.SESSION_LIMIT_EXCEEDED, sessionLimitResult.sessionInfoResponses());
            }

            sessionService.logoutSession(user.getId(), request.replaceSessionId());

            outboxService.save(
                    OutboxAggregateType.USER,
                    user.getId(),
                    OutboxEventType.AUDIT_EVENT,
                    new AuditEvent(
                            user.getId(),
                            null,
                            user.getLoginId(),
                            AuditEventType.LOGIN_SUCCESS,
                            ip,
                            userAgent
                    )
            );

        } else {
            SessionLimitResult sessionLimitResult = sessionLimitService.validateSessionLimit(user.getId(), user.getUserRole());

            if (sessionLimitResult.exceeded()) {
                throw new SessionLimitException(ErrorEnum.SESSION_LIMIT_EXCEEDED, sessionLimitResult.sessionInfoResponses());
            }
        }

        processLogin(user, ip, userAgent);

        LoginToken token = sessionService.createSession(user.getId(), ip, userAgent, user.getUserRole());

        return LoginResponse.from(user, token.accessToken(), token.refreshToken());
    }

    public void logout(String accessToken, HttpServletRequest servletRequest) {
        String ip = getUserIp(servletRequest);

        String userAgent = getUserAgent(servletRequest);

        LoginToken token = sessionService.logout(accessToken);

        outboxService.save(
                OutboxAggregateType.USER,
                token.userId(),
                OutboxEventType.AUDIT_EVENT,
                new AuditEvent(
                        token.userId(),
                        null,
                        null,
                        AuditEventType.LOGOUT,
                        ip,
                        userAgent
                )
        );
    }

    public LoginResponse refresh(RefreshRequest request, HttpServletRequest servletRequest) {
        String ip = getUserIp(servletRequest);

        String userAgent = getUserAgent(servletRequest);

        try {
            LoginToken token = sessionService.refresh(request.refreshToken());

            User user = userValidator.validateActiveUser(token.userId());

            return LoginResponse.from(user, token.accessToken(), token.refreshToken());

        } catch (BaseException e) {

            if (e.getErrorEnum() == ErrorEnum.REFRESH_TOKEN_REPLAY) {
                Long userId = jwtProvider.extractUserId(request.refreshToken());

                User user = userValidator.validateActiveUser(userId);

                outboxService.save(
                        OutboxAggregateType.USER,
                        user.getId(),
                        OutboxEventType.AUDIT_EVENT,
                        new AuditEvent(
                                user.getId(),
                                null,
                                user.getLoginId(),
                                AuditEventType.REFRESH_REPLAY,
                                ip,
                                userAgent
                        )
                );
            }

            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<SessionInfoResponse> findMySessions(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        return sessionService.findSessions(currentUserId);
    }

    public void logoutSession(Long currentUserId, String sessionId, HttpServletRequest servletRequest) {
        String ip = getUserIp(servletRequest);

        String userAgent = getUserAgent(servletRequest);

        userValidator.validateActiveUser(currentUserId);

        sessionService.logoutSession(currentUserId, sessionId);

        outboxService.save(
                OutboxAggregateType.USER,
                currentUserId,
                OutboxEventType.AUDIT_EVENT,
                new AuditEvent(
                        currentUserId,
                        null,
                        null,
                        AuditEventType.SESSION_LOGOUT,
                        ip,
                        userAgent
                )
        );
    }

    private void processLogin(User user, String ip, String userAgent) {
        try {
            loginSuccessService.loginSuccess(user, ip, userAgent);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BaseException(ErrorEnum.LOGIN_CONFLICT);
        }
    }

    private String getUserAgent(HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");

        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "UNKNOWN";
        }

        return userAgent;
    }

    private String getUserIp(HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();

        if (ip == null || ip.isBlank()) {
            ip = "UNKNOWN";
        }

        return ip;
    }
}
