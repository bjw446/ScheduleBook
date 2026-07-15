package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.redis.RedisSessionService;
import com.example.schedulebook.domain.auth.dto.properties.SessionLimitProperties;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import com.example.schedulebook.domain.auth.dto.response.SessionLimitResult;
import com.example.schedulebook.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SessionLimitService {

    private final RedisSessionService redisSessionService;
    private final SessionLimitProperties sessionLimitProperties;

    public SessionLimitResult validateSessionLimit(Long userId, UserRole userRole) {
        Set<String> sessions = redisSessionService.getSessions(userId);

        if (sessions == null) {
            return SessionLimitResult.available();
        }

        if (sessions.size() < sessionLimitProperties.getLimit(userRole)) {
            return SessionLimitResult.available();
        }

        List<SessionInfoResponse> sessionInfoResponses = redisSessionService.findAllSessionInfo(userId)
                .stream()
                .map(SessionInfoResponse::from)
                .toList();

        return SessionLimitResult.exceeded(sessionInfoResponses);
    }
}
