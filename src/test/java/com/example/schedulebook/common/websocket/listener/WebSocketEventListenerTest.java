package com.example.schedulebook.common.websocket.listener;

import com.example.schedulebook.common.redis.service.RedisPresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private RedisPresenceService redisPresenceService;

    @Mock
    private Principal principal;

    private WebSocketEventListener listener;

    private final Long userId = 1L;
    private final String sessionId = "session-123";

    @BeforeEach
    void setUp() {
        listener = new WebSocketEventListener(redisPresenceService);
    }

    @Test
    void CONNECT_시_인증된_사용자를_Presence에_등록한다() {
        // given
        when(principal.getName()).thenReturn(String.valueOf(userId));

        Message<byte[]> message = createConnectedMessage();

        SessionConnectedEvent event =
                new SessionConnectedEvent(this, message);

        // when
        listener.handleWebSocketConnect(event);

        // then
        verify(redisPresenceService)
                .register(userId, sessionId);
    }

    @Test
    void CONNECT_시_principal이_없으면_Presence에_등록하지_않는다() {
        // given
        Message<byte[]> message = createConnectedMessage(null);

        SessionConnectedEvent event =
                new SessionConnectedEvent(this, message);

        // when
        listener.handleWebSocketConnect(event);

        // then
        verifyNoInteractions(redisPresenceService);
    }

    @Test
    void DISCONNECT_시_sessionId로_사용자를_조회하고_세션을_제거한다() {
        // given
        when(redisPresenceService.findUser(sessionId))
                .thenReturn(userId);

        SessionDisconnectEvent event =
                createDisconnectEvent();

        // when
        listener.handleWebSocketDisconnect(event);

        // then
        verify(redisPresenceService)
                .findUser(sessionId);

        verify(redisPresenceService)
                .remove(userId, sessionId);
    }

    @Test
    void DISCONNECT_시_알_수_없는_session이면_세션을_제거하지_않는다() {
        // given
        when(redisPresenceService.findUser(sessionId))
                .thenReturn(null);

        SessionDisconnectEvent event =
                createDisconnectEvent();

        // when
        listener.handleWebSocketDisconnect(event);

        // then
        verify(redisPresenceService)
                .findUser(sessionId);

        verify(redisPresenceService, never())
                .remove(anyLong(), anyString());
    }

    @Test
    void DISCONNECT_중_Redis_예외가_발생해도_예외를_전파하지_않는다() {
        // given
        when(redisPresenceService.findUser(sessionId))
                .thenThrow(new RuntimeException("Redis unavailable"));

        SessionDisconnectEvent event =
                createDisconnectEvent();

        // when & then
        assertDoesNotThrow(
                () -> listener.handleWebSocketDisconnect(event)
        );

        verify(redisPresenceService)
                .findUser(sessionId);

        verify(redisPresenceService, never())
                .remove(anyLong(), anyString());
    }

    @Test
    void DISCONNECT_시_조회된_userId와_sessionId로_세션을_제거한다() {
        // given
        when(redisPresenceService.findUser(sessionId))
                .thenReturn(userId);

        SessionDisconnectEvent event =
                createDisconnectEvent();

        // when
        listener.handleWebSocketDisconnect(event);

        // then
        verify(redisPresenceService)
                .remove(userId, sessionId);
    }

    private Message<byte[]> createConnectedMessage() {
        return createConnectedMessage(principal);
    }

    private Message<byte[]> createConnectedMessage(Principal principal) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create();

        accessor.setSessionId(sessionId);

        if (principal != null) {
            accessor.setUser(principal);
        }

        return MessageBuilder
                .withPayload(new byte[0])
                .setHeaders(accessor)
                .build();
    }

    private SessionDisconnectEvent createDisconnectEvent() {
        Message<byte[]> message = MessageBuilder
                .withPayload(new byte[0])
                .build();

        return new SessionDisconnectEvent(
                this,
                message,
                sessionId,
                CloseStatus.NORMAL
        );
    }
}