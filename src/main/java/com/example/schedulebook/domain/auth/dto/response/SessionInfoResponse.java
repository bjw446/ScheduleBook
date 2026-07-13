package com.example.schedulebook.domain.auth.dto.response;

import java.time.LocalDateTime;

public record SessionInfoResponse(
        String sessionId,
        String ip,
        String userAgent,
        LocalDateTime loginAt,
        LocalDateTime lastAccessAt
) {
    public static SessionInfoResponse from(SessionInfo sessionInfo) {
        return new SessionInfoResponse(
                sessionInfo.sessionId(),
                sessionInfo.ip(),
                sessionInfo.userAgent(),
                sessionInfo.loginAt(),
                sessionInfo.lastAccessAt()
        );
    }
}
