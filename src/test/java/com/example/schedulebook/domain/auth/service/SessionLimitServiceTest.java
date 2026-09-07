package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.service.RedisSessionService;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.domain.auth.dto.properties.SessionLimitProperties;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.dto.response.SessionLimitResult;
import com.example.schedulebook.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionLimitServiceTest {

    @Mock
    private RedisSessionService redisSessionService;

    @Mock
    private SessionLimitProperties sessionLimitProperties;

    @Mock
    private SessionService sessionService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private SessionLimitService sessionLimitService;

    private Long userId;
    private UserRole userRole;
    private String sessionId;
    private String oldSessionId;
    private String newSessionId;
    private String operationId;

    private int sessionLimit;
    private Long refreshTokenExpiration;

    @BeforeEach
    void setUp() {
        userId = 1L;
        userRole = UserRole.USER;

        sessionId = "session-1";
        oldSessionId = "old-session";
        newSessionId = "new-session";
        operationId = "operation-1";

        sessionLimit = 5;
        refreshTokenExpiration = Duration.ofDays(7).toMillis();
    }

    @Test
    void given세션제한미만_whenReserveSession_then세션예약가능() {
        // given
        when(sessionLimitProperties.getLimit(userRole))
                .thenReturn(sessionLimit);

        when(jwtProperties.refreshTokenExpiration())
                .thenReturn(refreshTokenExpiration);

        when(redisSessionService.addSessionIfAvailable(
                userId,
                sessionId,
                sessionLimit,
                refreshTokenExpiration
        )).thenReturn(true);

        // when
        SessionLimitResult result =
                sessionLimitService.reserveSession(
                        userId,
                        userRole,
                        sessionId
                );

        // then
        assertThat(result.exceeded())
                .isFalse();

        assertThat(result.sessionInfoResponses())
                .isEmpty();

        verify(sessionLimitProperties)
                .getLimit(userRole);

        verify(jwtProperties)
                .refreshTokenExpiration();

        verify(redisSessionService)
                .addSessionIfAvailable(
                        userId,
                        sessionId,
                        sessionLimit,
                        refreshTokenExpiration
                );

        verifyNoInteractions(sessionService);
    }

    @Test
    void given세션제한도달_whenReserveSession_then조회된세션목록을결과에반환() {
        // given
        List<SessionInfoResponse> sessions = List.of(
                mock(SessionInfoResponse.class),
                mock(SessionInfoResponse.class)
        );

        when(sessionLimitProperties.getLimit(userRole))
                .thenReturn(3);

        when(jwtProperties.refreshTokenExpiration())
                .thenReturn(refreshTokenExpiration);

        when(redisSessionService.addSessionIfAvailable(
                userId,
                sessionId,
                3,
                refreshTokenExpiration
        )).thenReturn(false);

        when(sessionService.findSessions(userId))
                .thenReturn(sessions);

        // when
        SessionLimitResult result =
                sessionLimitService.reserveSession(userId, userRole, sessionId);

        // then
        assertThat(result.exceeded()).isTrue();
        assertThat(result.sessionInfoResponses())
                .isEqualTo(sessions);

        verify(redisSessionService).addSessionIfAvailable(
                userId,
                sessionId,
                3,
                refreshTokenExpiration
        );

        verify(sessionService).findSessions(userId);
    }

    @Test
    void givenRedis세션예약실패_whenReserveSession_then예외전파() {
        // given
        when(sessionLimitProperties.getLimit(userRole))
                .thenReturn(sessionLimit);

        when(jwtProperties.refreshTokenExpiration())
                .thenReturn(refreshTokenExpiration);

        RuntimeException exception =
                new RuntimeException("Redis unavailable");

        when(redisSessionService.addSessionIfAvailable(
                userId,
                sessionId,
                sessionLimit,
                refreshTokenExpiration
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                sessionLimitService.reserveSession(
                        userId,
                        userRole,
                        sessionId
                )
        )
                .isSameAs(exception);

        verify(redisSessionService)
                .addSessionIfAvailable(
                        userId,
                        sessionId,
                        sessionLimit,
                        refreshTokenExpiration
                );

        verifyNoInteractions(sessionService);
    }

    @Test
    void given기존세션교체가능_whenReplaceSession_then세션교체가능() {
        // given
        when(sessionLimitProperties.getLimit(userRole))
                .thenReturn(sessionLimit);

        when(jwtProperties.refreshTokenExpiration())
                .thenReturn(refreshTokenExpiration);

        when(redisSessionService.replaceSessionIfAvailable(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                sessionLimit,
                refreshTokenExpiration
        )).thenReturn(true);

        // when
        SessionLimitResult result =
                sessionLimitService.replaceSession(
                        userId,
                        userRole,
                        oldSessionId,
                        newSessionId,
                        operationId
                );

        // then
        assertThat(result.exceeded())
                .isFalse();

        assertThat(result.sessionInfoResponses())
                .isEmpty();

        verify(redisSessionService)
                .replaceSessionIfAvailable(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId,
                        sessionLimit,
                        refreshTokenExpiration
                );

        verifyNoInteractions(sessionService);
    }

    @Test
    void given세션교체불가_whenReplaceSession_then조회된세션목록을결과에반환() {
        // given
        List<SessionInfoResponse> sessions = List.of(
                mock(SessionInfoResponse.class),
                mock(SessionInfoResponse.class)
        );

        when(sessionLimitProperties.getLimit(userRole))
                .thenReturn(sessionLimit);

        when(jwtProperties.refreshTokenExpiration())
                .thenReturn(refreshTokenExpiration);

        when(redisSessionService.replaceSessionIfAvailable(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                sessionLimit,
                refreshTokenExpiration
        )).thenReturn(false);

        when(sessionService.findSessions(userId))
                .thenReturn(sessions);

        // when
        SessionLimitResult result =
                sessionLimitService.replaceSession(
                        userId,
                        userRole,
                        oldSessionId,
                        newSessionId,
                        operationId
                );

        // then
        assertThat(result.exceeded())
                .isTrue();

        assertThat(result.sessionInfoResponses())
                .isEqualTo(sessions);

        verify(redisSessionService)
                .replaceSessionIfAvailable(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId,
                        sessionLimit,
                        refreshTokenExpiration
                );

        verify(sessionService)
                .findSessions(userId);
    }

    @Test
    void givenRedis세션교체실패_whenReplaceSession_then예외전파() {
        // given
        when(sessionLimitProperties.getLimit(userRole))
                .thenReturn(sessionLimit);

        when(jwtProperties.refreshTokenExpiration())
                .thenReturn(refreshTokenExpiration);

        RuntimeException exception =
                new RuntimeException("Redis unavailable");

        when(redisSessionService.replaceSessionIfAvailable(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                sessionLimit,
                refreshTokenExpiration
        )).thenThrow(exception);

        // when & then
        assertThatThrownBy(() ->
                sessionLimitService.replaceSession(
                        userId,
                        userRole,
                        oldSessionId,
                        newSessionId,
                        operationId
                )
        )
                .isSameAs(exception);

        verify(redisSessionService)
                .replaceSessionIfAvailable(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId,
                        sessionLimit,
                        refreshTokenExpiration
                );

        verifyNoInteractions(sessionService);
    }
}