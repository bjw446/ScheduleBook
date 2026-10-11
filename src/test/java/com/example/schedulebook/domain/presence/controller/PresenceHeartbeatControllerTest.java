package com.example.schedulebook.domain.presence.controller;

import com.example.schedulebook.common.redis.service.RedisPresenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceHeartbeatControllerTest {

    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "session-123";

    @Mock
    private RedisPresenceService redisPresenceService;

    @Mock
    private SimpMessageHeaderAccessor accessor;

    @InjectMocks
    private PresenceHeartbeatController presenceHeartbeatController;

    @Test
    @DisplayName("인증된 사용자가 heartbeat를 전송하면 접속 상태를 갱신한다")
    void ping_인증된_사용자가_heartbeat를_전송하면_접속_상태를_갱신한다() {
        // given
        Principal principal = () -> String.valueOf(USER_ID);

        when(accessor.getSessionId()).thenReturn(SESSION_ID);

        // when
        presenceHeartbeatController.ping(principal, accessor);

        // then
        verify(redisPresenceService).refresh(USER_ID, SESSION_ID);
        verifyNoMoreInteractions(redisPresenceService);
    }

    @Test
    @DisplayName("인증 정보가 없으면 접속 상태를 갱신하지 않는다")
    void ping_인증_정보가_없으면_접속_상태를_갱신하지_않는다() {
        // given
        Principal principal = null;

        // when
        presenceHeartbeatController.ping(principal, accessor);

        // then
        verifyNoInteractions(redisPresenceService);
        verifyNoInteractions(accessor);
    }

    @Test
    @DisplayName("heartbeat를 전송한 사용자의 ID로 접속 상태를 갱신한다")
    void ping_인증된_사용자의_ID로_접속_상태를_갱신한다() {
        // given
        Long authenticatedUserId = 2L;
        String sessionId = "session-456";

        Principal principal = () -> String.valueOf(authenticatedUserId);

        when(accessor.getSessionId()).thenReturn(sessionId);

        // when
        presenceHeartbeatController.ping(principal, accessor);

        // then
        verify(redisPresenceService).refresh(authenticatedUserId, sessionId);
        verify(redisPresenceService, never()).refresh(USER_ID, sessionId);
        verifyNoMoreInteractions(redisPresenceService);
    }
}