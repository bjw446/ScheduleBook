package com.example.schedulebook.common.websocket;

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
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    public WebSocketEventListener(WebSocketSessionRegistry webSocketSessionRegistry) {
        this.webSocketSessionRegistry = webSocketSessionRegistry;
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

        webSocketSessionRegistry.register(accessor.getSessionId(), userId);

        log.info("WebSocket CONNECT userId = {}, sessionId = {}, userSessionCount = {}, onlineUsers = {}",
                userId,
                accessor.getSessionId(),
                webSocketSessionRegistry.getSessionCount(userId),
                webSocketSessionRegistry.getOnlineUserCount()
        );
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        Long userId = webSocketSessionRegistry.remove(event.getSessionId());

        if (userId == null) {
            log.warn("WebSocket DISCONNECT unknow sessionId  = {}", event.getSessionId());

            return;
        }

        log.info("WebSocket DISCONNECT userId = {}, sessionId = {}, remainingSessions = {}, onlineUsers = {}",
                userId,
                event.getSessionId(),
                webSocketSessionRegistry.getSessionCount(userId),
                webSocketSessionRegistry.getOnlineUserCount()
        );
    }
}
