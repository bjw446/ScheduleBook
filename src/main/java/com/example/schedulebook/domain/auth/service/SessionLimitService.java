package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.RedisSessionService;
import com.example.schedulebook.domain.auth.dto.properties.SessionLimitProperties;
import com.example.schedulebook.domain.auth.dto.response.SessionLimitResult;
import com.example.schedulebook.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SessionLimitService {

    private final RedisSessionService redisSessionService;
    private final SessionLimitProperties sessionLimitProperties;
    private final SessionService sessionService;

    public SessionLimitResult validateSessionLimit(Long userId, UserRole userRole) {
        Set<String> sessions = redisSessionService.getSessions(userId);

        if (sessions == null || sessions.size() < sessionLimitProperties.getLimit(userRole)) {
            return SessionLimitResult.available();
        }

        return SessionLimitResult.exceeded(sessionService.findSessions(userId));
    }

    public SessionLimitResult validateSessionLimitExcluding(Long userId, UserRole userRole, String sessionId) {
        Set<String> sessions = redisSessionService.getSessions(userId);

        int activeCount = sessions == null ? 0 : sessions.size();

        if (sessions != null  && sessions.contains(sessionId)) {
            activeCount--;
        }

        if (activeCount < sessionLimitProperties.getLimit(userRole)) {
            return SessionLimitResult.available();
        }

        return SessionLimitResult.exceeded(sessionService.findSessions(userId));
    }
}
