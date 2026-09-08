package com.example.schedulebook.domain.auth.dispatcher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.auth.dto.response.ForceLogoutResponse;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.service.SessionBlockStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForceLogoutDispatcherImplTest {

    @Mock
    private SessionBlockStore sessionBlockStore;

    @Mock
    private RedisPresenceService redisPresenceService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private ForceLogoutDispatcherImpl forceLogoutDispatcher;

    @Test
    void given오프라인사용자강제로그아웃이벤트_whenDispatch_then세션차단및메시지미전송() {
        // given
        Long userId = 1L;
        String sessionId = "session-123";
        long accessTokenExpiration = 60_000L;

        ForceLogoutSessionEvent event = new ForceLogoutSessionEvent(
                "event-123",
                userId,
                sessionId,
                accessTokenExpiration
        );

        given(redisPresenceService.isOnline(userId))
                .willReturn(false);

        // when
        forceLogoutDispatcher.dispatch(event);

        // then
        verify(sessionBlockStore)
                .block(sessionId, accessTokenExpiration);

        verify(redisPresenceService)
                .isOnline(userId);

        verify(simpMessagingTemplate, never())
                .convertAndSendToUser(
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void given온라인사용자_whenDispatch_then강제로그아웃메시지전송() {
        // given
        Long userId = 1L;
        String sessionId = "session-123";
        long accessTokenExpiration = 60_000L;

        ForceLogoutSessionEvent event = new ForceLogoutSessionEvent(
                "event-123",
                userId,
                sessionId,
                accessTokenExpiration
        );

        given(redisPresenceService.isOnline(userId))
                .willReturn(true);

        // when
        forceLogoutDispatcher.dispatch(event);

        // then
        verify(sessionBlockStore)
                .block(sessionId, accessTokenExpiration);

        verify(redisPresenceService)
                .isOnline(userId);

        ArgumentCaptor<ForceLogoutResponse> responseCaptor =
                ArgumentCaptor.forClass(ForceLogoutResponse.class);

        verify(simpMessagingTemplate).convertAndSendToUser(
                eq(userId.toString()),
                eq(WebSocketDestination.FORCE_LOGOUT),
                responseCaptor.capture()
        );

        ForceLogoutResponse response = responseCaptor.getValue();

        assertThat(response.sessionId())
                .isEqualTo(sessionId);
        assertThat(response.reason())
                .isEqualTo(AuditEventType.FORCE_LOGOUT.toString());
        assertThat(response.message())
                .isEqualTo("다른 환경에서 로그아웃되었습니다.");
    }

    @Test
    void given온라인사용자_when강제로그아웃메시지전송실패_then예외전파() {
        // given
        Long userId = 1L;
        String sessionId = "session-123";
        long accessTokenExpiration = 60_000L;

        ForceLogoutSessionEvent event = new ForceLogoutSessionEvent(
                "event-123",
                userId,
                sessionId,
                accessTokenExpiration
        );

        RuntimeException exception =
                new RuntimeException("WebSocket send failed");

        given(redisPresenceService.isOnline(userId))
                .willReturn(true);

        doThrow(exception)
                .when(simpMessagingTemplate)
                .convertAndSendToUser(
                        eq(userId.toString()),
                        eq(WebSocketDestination.FORCE_LOGOUT),
                        any(ForceLogoutResponse.class)
                );

        // when & then
        assertThatThrownBy(() ->
                forceLogoutDispatcher.dispatch(event)
        )
                .isSameAs(exception);

        verify(sessionBlockStore)
                .block(sessionId, accessTokenExpiration);

        verify(redisPresenceService)
                .isOnline(userId);

        verify(simpMessagingTemplate)
                .convertAndSendToUser(
                        eq(userId.toString()),
                        eq(WebSocketDestination.FORCE_LOGOUT),
                        any(ForceLogoutResponse.class)
                );
    }
}