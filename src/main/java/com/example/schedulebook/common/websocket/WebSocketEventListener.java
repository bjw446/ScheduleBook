package com.example.schedulebook.common.websocket;

import com.example.schedulebook.common.redis.service.RedisPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@Slf4j
public class WebSocketEventListener {
    private final RedisPresenceService redisPresenceService;

    public WebSocketEventListener(RedisPresenceService redisPresenceService) {
        this.redisPresenceService = redisPresenceService;
    }

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

        Principal principal = accessor.getUser();

        if (principal == null) {
            log.warn("WebSocket CONNECT 인증 정보 없음");

            return;
        }

        Long userId = Long.valueOf(principal.getName());

        redisPresenceService.register(userId, accessor.getSessionId());

        log.info("WebSocket CONNECT userId = {}, sessionId = {}, userSessionCount = {}",
                userId,
                accessor.getSessionId(),
                redisPresenceService.getSessionCount(userId)
        );
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        Long userId = redisPresenceService.findUser(event.getSessionId());

        if (userId == null) {
            log.warn("WebSocket DISCONNECT unknown sessionId = {}", event.getSessionId());

            return;
        }

        redisPresenceService.remove(userId, event.getSessionId());

        log.info("WebSocket DISCONNECT userId = {}, sessionId = {}, remainingSessions = {}",
                userId,
                event.getSessionId(),
                redisPresenceService.getSessionCount(userId)
        );
    }
}
