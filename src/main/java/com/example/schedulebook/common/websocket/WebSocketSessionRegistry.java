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
        Long previousUserId = sessionUserMap.put(sessionId, userId);

        if (previousUserId != null && !previousUserId.equals(userId)) {
            userSessionsMap.computeIfPresent(previousUserId, (id, sessions) -> {
                sessions.remove(sessionId);

                return sessions.isEmpty() ? null : sessions;
            });
        }

        userSessionsMap.compute(userId, (id, sessions) -> {
            if (sessions == null) {
                sessions = ConcurrentHashMap.newKeySet();
            }

            sessions.add(sessionId);

            return sessions;
        });
    }

    public Long remove(String sessionId) {
        Long userId = sessionUserMap.remove(sessionId);

        if (userId == null) {
            return null;
        }

        userSessionsMap.computeIfPresent(userId, (id, sessions) -> {
            sessions.remove(sessionId);

            if (sessions.isEmpty()) {
                sessionUserMap.remove(sessionId);

                return null;
            }

            return sessions;
        });

        sessionUserMap.remove(sessionId);

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
