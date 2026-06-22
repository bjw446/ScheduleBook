package com.example.schedulebook.domain.presence.service;

import com.example.schedulebook.common.websocket.WebSocketSessionRegistry;
import com.example.schedulebook.domain.presence.dto.response.UserPresenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    @Transactional(readOnly = true)
    public UserPresenceResponse findPresence(Long userId) {
        return new UserPresenceResponse(
                userId,
                webSocketSessionRegistry.isOnline(userId),
                webSocketSessionRegistry.getSessionCount(userId)
        );
    }
}
