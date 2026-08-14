package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.service.RedisSessionService;
import com.example.schedulebook.common.security.JwtProperties;
import com.example.schedulebook.domain.auth.dto.properties.SessionLimitProperties;
import com.example.schedulebook.domain.auth.dto.response.SessionLimitResult;
import com.example.schedulebook.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SessionLimitService {

    private final RedisSessionService redisSessionService;
    private final SessionLimitProperties sessionLimitProperties;
    private final SessionService sessionService;
    private final JwtProperties jwtProperties;

    public SessionLimitResult reserveSession(Long userId, UserRole userRole, String sessionId) {
        int limit = sessionLimitProperties.getLimit(userRole);

        boolean available = redisSessionService.addSessionIfAvailable(
                userId,
                sessionId,
                limit,
                jwtProperties.refreshTokenExpiration()
        );

        if (available) {
            return SessionLimitResult.available();
        }

        return SessionLimitResult.exceeded(sessionService.findSessions(userId));
    }

    public SessionLimitResult replaceSession(
            Long userId,
            UserRole userRole,
            String oldSessionId,
            String newSessionId,
            String operationId
    ) {
        int limit = sessionLimitProperties.getLimit(userRole);

        boolean available = redisSessionService.replaceSessionIfAvailable(
                userId,
                oldSessionId,
                newSessionId,
                operationId,
                limit,
                jwtProperties.refreshTokenExpiration()
        );

        if (available) {
            return SessionLimitResult.available();
        }

        return SessionLimitResult.exceeded(sessionService.findSessions(userId));
    }
}
