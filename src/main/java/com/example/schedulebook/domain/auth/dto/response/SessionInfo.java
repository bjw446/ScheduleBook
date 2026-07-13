package com.example.schedulebook.domain.auth.dto.response;

import java.time.LocalDateTime;

public record SessionInfo(
        Long userId,
        String sessionId,
        String ip,
        String userAgent,
        LocalDateTime loginAt,
        LocalDateTime lastAccessAt
) {
    public static SessionInfo create(
            Long userId,
            String sessionId,
            String ip,
            String userAgent
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new SessionInfo(
                userId,
                sessionId,
                ip,
                userAgent,
                now,
                now
        );
    }
}
