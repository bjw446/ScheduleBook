package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.service.RedisBlacklistService;
import com.example.schedulebook.common.redis.service.RedisRefreshTokenService;
import com.example.schedulebook.common.redis.service.RedisSessionService;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.common.security.JwtProvider;
import com.example.schedulebook.domain.auth.dto.response.SessionInfo;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.dto.token.LoginToken;
import com.example.schedulebook.domain.auth.enums.RefreshRotateResult;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    JwtProvider jwtProvider;

    @Mock
    JwtProperties jwtProperties;

    @Mock
    RedisRefreshTokenService redisRefreshTokenService;

    @Mock
    RedisSessionService redisSessionService;

    @Mock
    RedisBlacklistService redisBlacklistService;

    @Mock
    OutboxService outboxService;

    private SessionService sessionService;

    private final Long userId = 1L;
    private final String sessionId = "session-id";
    private final String oldSessionId = "old-session-id";
    private final String newSessionId = "new-session-id";
    private final String operationId = "operation-id";

    private final String accessToken = "access-token";
    private final String refreshToken = "refresh-token";
    private final String newRefreshToken = "new-refresh-token";

    private final String ip = "127.0.0.1";
    private final String userAgent = "test-agent";

    private final UserRole userRole = UserRole.USER;

    private final long refreshExpiration = 1_209_600_000L;
    private final long accessExpiration = 3_600_000L;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                jwtProvider,
                jwtProperties,
                redisRefreshTokenService,
                redisSessionService,
                redisBlacklistService,
                outboxService
        );

        lenient().when(jwtProperties.refreshTokenExpiration())
                .thenReturn(refreshExpiration);

        lenient().when(jwtProperties.accessTokenExpiration())
                .thenReturn(accessExpiration);
    }

    @Test
    void given유효한_사용자와_세션정보_when세션생성_then토큰과_Redis세션정보를_저장하고_로그인토큰을_반환한다() {
        when(jwtProvider.generateAccessToken(userId, sessionId, userRole))
                .thenReturn(accessToken);

        when(jwtProvider.generateRefreshToken(userId, sessionId, userRole))
                .thenReturn(refreshToken);

        LoginToken result = sessionService.createSession(
                userId,
                sessionId,
                ip,
                userAgent,
                userRole
        );

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);

        verify(jwtProvider).generateAccessToken(userId, sessionId, userRole);
        verify(jwtProvider).generateRefreshToken(userId, sessionId, userRole);

        verify(redisRefreshTokenService).saveRefreshToken(
                sessionId,
                refreshToken,
                refreshExpiration
        );

        ArgumentCaptor<SessionInfo> sessionInfoCaptor =
                ArgumentCaptor.forClass(SessionInfo.class);

        verify(redisSessionService).saveSessionInfo(
                sessionInfoCaptor.capture(),
                eq(refreshExpiration)
        );

        SessionInfo sessionInfo = sessionInfoCaptor.getValue();

        assertThat(sessionInfo.userId()).isEqualTo(userId);
        assertThat(sessionInfo.sessionId()).isEqualTo(sessionId);
        assertThat(sessionInfo.ip()).isEqualTo(ip);
        assertThat(sessionInfo.userAgent()).isEqualTo(userAgent);
    }

    @Test
    void givenIP와_UserAgent가_null_when세션생성_then_unknown으로_저장한다() {
        when(jwtProvider.generateAccessToken(userId, sessionId, userRole))
                .thenReturn(accessToken);

        when(jwtProvider.generateRefreshToken(userId, sessionId, userRole))
                .thenReturn(refreshToken);

        sessionService.createSession(
                userId,
                sessionId,
                null,
                null,
                userRole
        );

        ArgumentCaptor<SessionInfo> captor =
                ArgumentCaptor.forClass(SessionInfo.class);

        verify(redisSessionService).saveSessionInfo(
                captor.capture(),
                eq(refreshExpiration)
        );

        SessionInfo sessionInfo = captor.getValue();

        assertThat(sessionInfo.ip()).isEqualTo("unknown");
        assertThat(sessionInfo.userAgent()).isEqualTo("unknown");
    }

    @Test
    void given유효한_accessToken과_블랙리스트미등록_when로그아웃_then세션을_삭제하고_블랙리스트를_저장한다() {
        when(jwtProvider.extractUserId(accessToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(accessToken)).thenReturn(sessionId);
        when(jwtProvider.getRemainingTime(accessToken)).thenReturn(accessExpiration);
        when(redisBlacklistService.isBlacklisted(accessToken)).thenReturn(false);

        LoginToken result = sessionService.logout(accessToken);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isNull();

        verify(jwtProvider).validateToken(accessToken);

        verify(redisRefreshTokenService).deleteRefreshToken(sessionId);
        verify(redisSessionService).deleteSessionInfo(sessionId);
        verify(redisSessionService).removeSession(userId, sessionId);

        verify(redisBlacklistService).isBlacklisted(accessToken);
        verify(redisBlacklistService).saveBlacklistToken(
                accessToken,
                accessExpiration
        );
    }

    @Test
    void given이미_블랙리스트에_등록된_accessToken_when로그아웃_then블랙리스트를_중복_저장하지_않는다() {
        when(jwtProvider.extractUserId(accessToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(accessToken)).thenReturn(sessionId);
        when(jwtProvider.getRemainingTime(accessToken)).thenReturn(accessExpiration);
        when(redisBlacklistService.isBlacklisted(accessToken)).thenReturn(true);

        sessionService.logout(accessToken);

        verify(redisBlacklistService).isBlacklisted(accessToken);
        verify(redisBlacklistService, never())
                .saveBlacklistToken(anyString(), anyLong());
    }

    @Test
    void given유효하지_않은_accessToken_when로그아웃_then세션과_블랙리스트를_처리하지_않는다() {
        doThrow(new BaseException(ErrorEnum.TOKEN_INVALID))
                .when(jwtProvider)
                .validateToken(accessToken);

        assertThatThrownBy(() -> sessionService.logout(accessToken))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.TOKEN_INVALID);

        verifyNoInteractions(
                redisRefreshTokenService,
                redisSessionService,
                redisBlacklistService
        );

        verify(jwtProvider, never()).extractUserId(anyString());
        verify(jwtProvider, never()).extractSessionId(anyString());
    }

    @Test
    void given유효한_refreshToken_when갱신_then_refreshToken을_rotation하고_세션정보를_갱신한다() {
        when(jwtProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(refreshToken)).thenReturn(sessionId);
        when(jwtProvider.extractUserRole(refreshToken)).thenReturn(userRole);

        when(redisSessionService.isSessionMember(userId, sessionId))
                .thenReturn(true);

        when(redisSessionService.existsSession(sessionId))
                .thenReturn(true);

        when(redisRefreshTokenService.hasRefreshToken(sessionId))
                .thenReturn(true);

        when(jwtProvider.generateRefreshToken(userId, sessionId, userRole))
                .thenReturn(newRefreshToken);

        when(redisRefreshTokenService.rotateRefreshToken(
                sessionId,
                refreshToken,
                newRefreshToken,
                refreshExpiration
        )).thenReturn(RefreshRotateResult.SUCCESS);

        when(jwtProvider.generateAccessToken(userId, sessionId, userRole))
                .thenReturn(accessToken);

        LoginToken result = sessionService.refresh(refreshToken);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);

        verify(jwtProvider).validateToken(refreshToken);

        verify(redisRefreshTokenService).rotateRefreshToken(
                sessionId,
                refreshToken,
                newRefreshToken,
                refreshExpiration
        );

        verify(redisSessionService).extendSessionTTL(
                userId,
                sessionId,
                refreshExpiration
        );

        verify(redisSessionService).updateLastAccess(sessionId);
    }

    @Test
    void givenRedis에_refreshToken이_없는_세션_when갱신_thenREFRESH_TOKEN_INVALID를_던진다() {
        when(jwtProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(refreshToken)).thenReturn(sessionId);

        when(redisSessionService.isSessionMember(userId, sessionId))
                .thenReturn(true);

        when(redisSessionService.existsSession(sessionId))
                .thenReturn(true);

        when(redisRefreshTokenService.hasRefreshToken(sessionId))
                .thenReturn(false);

        assertThatThrownBy(() -> sessionService.refresh(refreshToken))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.REFRESH_TOKEN_INVALID);

        verify(redisRefreshTokenService, never())
                .rotateRefreshToken(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void given_refreshToken의_세션이_없는_경우_when갱신_thenSESSION_NOT_FOUND를_던진다() {
        when(jwtProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(refreshToken)).thenReturn(sessionId);

        when(redisSessionService.isSessionMember(userId, sessionId))
                .thenReturn(false);

        assertThatThrownBy(() -> sessionService.refresh(refreshToken))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SESSION_NOT_FOUND);

        verify(redisSessionService, never()).existsSession(anyString());
        verify(redisRefreshTokenService, never()).hasRefreshToken(anyString());
    }

    @Test
    void given_rotation대상_refreshToken이_없는_경우_when갱신_thenREFRESH_TOKEN_INVALID를_던진다() {
        when(jwtProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(refreshToken)).thenReturn(sessionId);
        when(jwtProvider.extractUserRole(refreshToken)).thenReturn(userRole);

        when(redisSessionService.isSessionMember(userId, sessionId))
                .thenReturn(true);

        when(redisSessionService.existsSession(sessionId))
                .thenReturn(true);

        when(redisRefreshTokenService.hasRefreshToken(sessionId))
                .thenReturn(true);

        when(jwtProvider.generateRefreshToken(userId, sessionId, userRole))
                .thenReturn(newRefreshToken);

        when(redisRefreshTokenService.rotateRefreshToken(
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        )).thenReturn(RefreshRotateResult.NOT_FOUND);

        assertThatThrownBy(() -> sessionService.refresh(refreshToken))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.REFRESH_TOKEN_INVALID);

        verify(redisSessionService, never())
                .extendSessionTTL(anyLong(), anyString(), anyLong());

        verify(redisSessionService, never())
                .updateLastAccess(anyString());
    }

    @Test
    void given_refreshToken_rotation중_토큰이_불일치하면_when갱신_then전체세션을_삭제하고_REPLAY를_던진다() {
        when(jwtProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(refreshToken)).thenReturn(sessionId);
        when(jwtProvider.extractUserRole(refreshToken)).thenReturn(userRole);

        when(redisSessionService.isSessionMember(userId, sessionId))
                .thenReturn(true);

        when(redisSessionService.existsSession(sessionId))
                .thenReturn(true);

        when(redisRefreshTokenService.hasRefreshToken(sessionId))
                .thenReturn(true);

        when(jwtProvider.generateRefreshToken(userId, sessionId, userRole))
                .thenReturn(newRefreshToken);

        when(redisRefreshTokenService.rotateRefreshToken(
                sessionId,
                refreshToken,
                newRefreshToken,
                refreshExpiration
        )).thenReturn(RefreshRotateResult.TOKEN_MISMATCH);

        assertThatThrownBy(() -> sessionService.refresh(refreshToken))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.REFRESH_TOKEN_REPLAY);

        verify(redisSessionService).deleteAllSessions(userId);

        verify(redisSessionService, never())
                .extendSessionTTL(anyLong(), anyString(), anyLong());

        verify(redisSessionService, never())
                .updateLastAccess(anyString());
    }

    @Test
    void given_refresh_중_TTL연장에서_예외가_발생해도_when갱신_then정상적으로_토큰을_반환한다() {
        when(jwtProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(refreshToken)).thenReturn(sessionId);
        when(jwtProvider.extractUserRole(refreshToken)).thenReturn(userRole);

        when(redisSessionService.isSessionMember(userId, sessionId))
                .thenReturn(true);

        when(redisSessionService.existsSession(sessionId))
                .thenReturn(true);

        when(redisRefreshTokenService.hasRefreshToken(sessionId))
                .thenReturn(true);

        when(jwtProvider.generateRefreshToken(userId, sessionId, userRole))
                .thenReturn(newRefreshToken);

        when(redisRefreshTokenService.rotateRefreshToken(
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        )).thenReturn(RefreshRotateResult.SUCCESS);

        when(jwtProvider.generateAccessToken(userId, sessionId, userRole))
                .thenReturn(accessToken);

        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisSessionService)
                .extendSessionTTL(userId, sessionId, refreshExpiration);

        // when
        LoginToken result = sessionService.refresh(refreshToken);

        // then
        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);

        verify(redisSessionService).extendSessionTTL(
                userId,
                sessionId,
                refreshExpiration
        );

        verify(redisSessionService).updateLastAccess(sessionId);
    }

    @Test
    void given_refresh_중_최근접근시간_갱신에서_예외가_발생해도_when갱신_then정상적으로_토큰을_반환한다() {
        when(jwtProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.extractSessionId(refreshToken)).thenReturn(sessionId);
        when(jwtProvider.extractUserRole(refreshToken)).thenReturn(userRole);

        when(redisSessionService.isSessionMember(userId, sessionId))
                .thenReturn(true);

        when(redisSessionService.existsSession(sessionId))
                .thenReturn(true);

        when(redisRefreshTokenService.hasRefreshToken(sessionId))
                .thenReturn(true);

        when(jwtProvider.generateRefreshToken(userId, sessionId, userRole))
                .thenReturn(newRefreshToken);

        when(redisRefreshTokenService.rotateRefreshToken(
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        )).thenReturn(RefreshRotateResult.SUCCESS);

        when(jwtProvider.generateAccessToken(userId, sessionId, userRole))
                .thenReturn(accessToken);

        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisSessionService)
                .updateLastAccess(sessionId);

        // when
        LoginToken result = sessionService.refresh(refreshToken);

        // then
        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);

        verify(redisSessionService).extendSessionTTL(
                userId,
                sessionId,
                refreshExpiration
        );

        verify(redisSessionService).updateLastAccess(sessionId);
    }

    @Test
    void given여러_세션이_존재할때_when내_세션조회_then_loginAt_내림차순으로_반환한다() {
        LocalDateTime olderLoginAt = LocalDateTime.of(
                2026, 9, 3, 14, 0
        );

        LocalDateTime newerLoginAt = LocalDateTime.of(
                2026, 9, 3, 15, 0
        );

        SessionInfo older = new SessionInfo(
                userId,
                "old-session",
                ip,
                userAgent,
                olderLoginAt,
                olderLoginAt
        );

        SessionInfo newer = new SessionInfo(
                userId,
                "new-session",
                ip,
                userAgent,
                newerLoginAt,
                newerLoginAt
        );

        when(redisSessionService.getSessions(userId))
                .thenReturn(Set.of("old-session", "new-session"));

        when(redisSessionService.getSessionInfo("old-session"))
                .thenReturn(Optional.of(older));

        when(redisSessionService.getSessionInfo("new-session"))
                .thenReturn(Optional.of(newer));

        List<SessionInfoResponse> result =
                sessionService.findSessions(userId);

        assertThat(result)
                .hasSize(2)
                .extracting(SessionInfoResponse::sessionId)
                .containsExactly(
                        "new-session",
                        "old-session"
                );
    }

    @Test
    void given세션이_없는_사용자_when내_세션조회_then빈_리스트를_반환한다() {
        when(redisSessionService.getSessions(userId))
                .thenReturn(Set.of());

        List<SessionInfoResponse> result =
                sessionService.findSessions(userId);

        assertThat(result).isEmpty();

        verify(redisSessionService, never()).getSessionInfo(anyString());
    }

    @Test
    void given세션목록중_세션정보가_없는_경우_when내_세션조회_then존재하는_세션만_반환한다() {
        SessionInfo sessionInfo = SessionInfo.create(
                userId,
                sessionId,
                ip,
                userAgent
        );

        when(redisSessionService.getSessions(userId))
                .thenReturn(Set.of(sessionId, "deleted-session"));

        when(redisSessionService.getSessionInfo(sessionId))
                .thenReturn(Optional.of(sessionInfo));

        when(redisSessionService.getSessionInfo("deleted-session"))
                .thenReturn(Optional.empty());

        List<SessionInfoResponse> result =
                sessionService.findSessions(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionId())
                .isEqualTo(sessionId);
    }

    @Test
    void givenRedis에서_세션목록이_null일때_when내_세션조회_then빈_리스트를_반환한다() {
        when(redisSessionService.getSessions(userId))
                .thenReturn(null);

        List<SessionInfoResponse> result =
                sessionService.findSessions(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void given사용자_본인의_세션_when세션로그아웃_then세션을_삭제한다() {
        SessionInfo sessionInfo = SessionInfo.create(
                userId,
                sessionId,
                ip,
                userAgent
        );

        when(redisSessionService.getSessionInfo(sessionId))
                .thenReturn(Optional.of(sessionInfo));

        sessionService.logoutSession(userId, sessionId);

        verify(redisSessionService).getSessionInfo(sessionId);

        verify(redisRefreshTokenService).deleteRefreshToken(sessionId);
        verify(redisSessionService).deleteSessionInfo(sessionId);
        verify(redisSessionService).removeSession(userId, sessionId);
    }

    @Test
    void given존재하지_않는_세션_when세션로그아웃_thenSESSION_NOT_FOUND를_던진다() {
        when(redisSessionService.getSessionInfo(sessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                sessionService.logoutSession(userId, sessionId))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.SESSION_NOT_FOUND);

        verify(redisRefreshTokenService, never())
                .deleteRefreshToken(anyString());

        verify(redisSessionService, never())
                .removeSession(anyLong(), anyString());
    }

    @Test
    void given다른_사용자의_세션_when세션로그아웃_thenFORBIDDEN을_던진다() {
        Long ownerId = 999L;

        SessionInfo sessionInfo = SessionInfo.create(
                ownerId,
                sessionId,
                ip,
                userAgent
        );

        when(redisSessionService.getSessionInfo(sessionId))
                .thenReturn(Optional.of(sessionInfo));

        assertThatThrownBy(() ->
                sessionService.logoutSession(userId, sessionId))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.FORBIDDEN);

        verify(redisRefreshTokenService, never())
                .deleteRefreshToken(anyString());

        verify(redisSessionService, never())
                .deleteSessionInfo(anyString());

        verify(redisSessionService, never())
                .removeSession(anyLong(), anyString());
    }

    @Test
    void given본인_세션_when단일_강제로그아웃_then세션을_삭제하고_FORCE_LOGOUT_Outbox를_저장한다() {
        SessionInfo sessionInfo = SessionInfo.create(
                userId,
                sessionId,
                ip,
                userAgent
        );

        when(redisSessionService.getSessionInfo(sessionId))
                .thenReturn(Optional.of(sessionInfo));

        sessionService.forceLogoutSession(userId, sessionId);

        verify(redisSessionService).removeSession(userId, sessionId);

        ArgumentCaptor<String> eventIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<OutboxAggregateType> aggregateCaptor =
                ArgumentCaptor.forClass(OutboxAggregateType.class);

        ArgumentCaptor<String> aggregateIdCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<OutboxEventType> eventTypeCaptor =
                ArgumentCaptor.forClass(OutboxEventType.class);

        ArgumentCaptor<Object> payloadCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(outboxService).save(
                eventIdCaptor.capture(),
                aggregateCaptor.capture(),
                aggregateIdCaptor.capture(),
                eventTypeCaptor.capture(),
                payloadCaptor.capture()
        );

        assertThat(aggregateCaptor.getValue())
                .isEqualTo(OutboxAggregateType.SESSION);

        assertThat(aggregateIdCaptor.getValue())
                .isEqualTo(sessionId);

        assertThat(eventTypeCaptor.getValue())
                .isEqualTo(OutboxEventType.FORCE_LOGOUT);

        assertThat(payloadCaptor.getValue())
                .isInstanceOf(ForceLogoutSessionEvent.class);

        ForceLogoutSessionEvent event =
                (ForceLogoutSessionEvent) payloadCaptor.getValue();

        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.sessionId()).isEqualTo(sessionId);
        assertThat(event.accessTokenExpiration())
                .isEqualTo(accessExpiration);
    }

    @Test
    void given여러_세션이_존재할때_when전체_강제로그아웃_then세션을_모두_삭제하고_각각_Outbox를_저장한다() {
        Set<String> sessionIds =
                Set.of("session-1", "session-2", "session-3");

        when(redisSessionService.getSessions(userId))
                .thenReturn(sessionIds);

        sessionService.forceLogoutAllSessions(userId);

        verify(redisSessionService)
                .incrementSessionGeneration(userId);

        verify(redisSessionService, times(3))
                .removeSession(eq(userId), anyString());

        verify(outboxService, times(3)).save(
                anyString(),
                eq(OutboxAggregateType.SESSION),
                anyString(),
                eq(OutboxEventType.FORCE_LOGOUT),
                any(ForceLogoutSessionEvent.class)
        );
    }

    @Test
    void given세션이_없는_사용자_when전체_강제로그아웃_then세션삭제없이_종료한다() {
        when(redisSessionService.getSessions(userId))
                .thenReturn(Set.of());

        sessionService.forceLogoutAllSessions(userId);

        verify(redisSessionService)
                .incrementSessionGeneration(userId);

        verify(redisSessionService, never())
                .removeSession(anyLong(), anyString());

        verifyNoInteractions(outboxService);
    }

    @Test
    void givenRedis에서_세션목록이_null일때_when전체_강제로그아웃_then세션삭제없이_종료한다() {
        when(redisSessionService.getSessions(userId))
                .thenReturn(null);

        sessionService.forceLogoutAllSessions(userId);

        verify(redisSessionService)
                .incrementSessionGeneration(userId);

        verify(redisSessionService, never())
                .removeSession(anyLong(), anyString());

        verifyNoInteractions(outboxService);
    }

    @Test
    void when세션ID를_생성하면_thenUUID형식의_서로_다른_ID를_반환한다() {
        String first = sessionService.generateSessionId();
        String second = sessionService.generateSessionId();

        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
        assertThat(first).isNotEqualTo(second);

        assertThat(first).matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-" +
                        "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
        );
    }

    @Test
    void given사용자와_세션ID_when세션삭제_then_refreshToken과_세션정보와_세션목록에서_모두삭제한다() {
        sessionService.removeSession(userId, sessionId);

        verify(redisRefreshTokenService)
                .deleteRefreshToken(sessionId);

        verify(redisSessionService)
                .deleteSessionInfo(sessionId);

        verify(redisSessionService)
                .removeSession(userId, sessionId);
    }

    @Test
    void given교체된_세션의_롤백이_성공하면_when롤백_then기존세션을_복구하고_신규세션과_pending을_정리한다() {
        when(redisSessionService.revertReplaceSession(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                refreshExpiration
        )).thenReturn(true);

        sessionService.rollbackReplacedSession(
                userId,
                oldSessionId,
                newSessionId,
                operationId
        );

        verify(redisSessionService).revertReplaceSession(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                refreshExpiration
        );

        verify(redisRefreshTokenService)
                .deleteRefreshToken(newSessionId);

        verify(redisSessionService)
                .deleteSessionInfo(newSessionId);

        verify(redisSessionService)
                .removeSession(userId, newSessionId);

        verify(redisSessionService)
                .deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );
    }

    @Test
    void given교체세션_복구가_false를_반환하면_when롤백_then보상정리를_수행하고_예외를_전파하지_않는다() {
        when(redisSessionService.revertReplaceSession(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                refreshExpiration
        )).thenReturn(false);

        // when & then
        assertThatCode(() ->
                sessionService.rollbackReplacedSession(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId
                )
        ).doesNotThrowAnyException();

        verify(redisRefreshTokenService)
                .deleteRefreshToken(newSessionId);

        verify(redisSessionService)
                .deleteSessionInfo(newSessionId);

        verify(redisSessionService)
                .removeSession(userId, newSessionId);

        verify(redisSessionService)
                .deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );
    }

    @Test
    void given교체세션_복구중_Redis예외가_발생해도_when롤백_then신규세션과_pending을_정리한다() {
        when(redisSessionService.revertReplaceSession(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                refreshExpiration
        )).thenThrow(new RuntimeException("Redis unavailable"));

        // when & then
        assertThatCode(() ->
                sessionService.rollbackReplacedSession(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId
                )
        ).doesNotThrowAnyException();

        verify(redisRefreshTokenService)
                .deleteRefreshToken(newSessionId);

        verify(redisSessionService)
                .deleteSessionInfo(newSessionId);

        verify(redisSessionService)
                .removeSession(userId, newSessionId);

        verify(redisSessionService)
                .deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );
    }

    @Test
    void given_rollback중_신규세션_삭제에서_예외가_발생해도_when롤백_thenpending_정리를_계속_수행한다() {
        when(redisSessionService.revertReplaceSession(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        )).thenReturn(false);

        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisSessionService)
                .removeSession(userId, newSessionId);

        // when & then
        assertThatCode(() ->
                sessionService.rollbackReplacedSession(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId
                )
        ).doesNotThrowAnyException();

        verify(redisRefreshTokenService)
                .deleteRefreshToken(newSessionId);

        verify(redisSessionService)
                .deleteSessionInfo(newSessionId);

        verify(redisSessionService)
                .removeSession(userId, newSessionId);

        verify(redisSessionService)
                .deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );
    }

    @Test
    void given_rollback중_pending_삭제에서_예외가_발생해도_when롤백_then예외를_전파하지_않는다() {
        when(redisSessionService.revertReplaceSession(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        )).thenReturn(false);

        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisSessionService)
                .deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );

        // when & then
        assertThatCode(() ->
                sessionService.rollbackReplacedSession(
                        userId,
                        oldSessionId,
                        newSessionId,
                        operationId
                )
        ).doesNotThrowAnyException();

        verify(redisRefreshTokenService)
                .deleteRefreshToken(newSessionId);

        verify(redisSessionService)
                .deleteSessionInfo(newSessionId);

        verify(redisSessionService)
                .removeSession(userId, newSessionId);

        verify(redisSessionService)
                .deleteReplacePendingIfOwner(
                        userId,
                        oldSessionId,
                        operationId
                );
    }

    @Test
    void given기존_세션ID_when교체세션_cleanup_then_refreshToken과_세션정보를_삭제한다() {
        sessionService.cleanupReplacedSession(oldSessionId);

        verify(redisRefreshTokenService)
                .deleteRefreshToken(oldSessionId);

        verify(redisSessionService)
                .deleteSessionInfo(oldSessionId);
    }
}