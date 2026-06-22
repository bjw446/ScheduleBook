package com.example.schedulebook.common.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    private final Map<Long, Set<String>> userSessionsMap = new ConcurrentHashMap<>();

    public void register(String sessionId, Long userId) {
        sessionUserMap.put(sessionId, userId);

        userSessionsMap.computeIfAbsent(
                userId,
                ket -> ConcurrentHashMap.newKeySet()
                )
                .add(sessionId);
    }

    public Long remove(String sessionId) {
        Long userId = sessionUserMap.remove(sessionId);

        if (userId == null) {
            return null;
        }

        Set<String> sessions = userSessionsMap.get(userId);

        if (sessions != null) {
            sessions.remove(sessionId);

            if (sessions.isEmpty()) {
                userSessionsMap.remove(userId);
            }
        }

        return userId;
    }

    public Long findUser(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    public boolean isOnline(Long userId) {
        return userSessionsMap.containsKey(userId);
    }

    public int getSessionCount(Long userId) {
        Set<String> sessions = userSessionsMap.get(userId);

        return sessions == null ? 0 : sessions.size();
    }

    public int getOnlineUserCount() {
        return userSessionsMap.size();
    }
}
