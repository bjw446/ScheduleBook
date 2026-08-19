package com.example.schedulebook.domain.auth.event;

public record ForceLogoutSessionEvent(
        String eventId,
        Long userId,
        String sessionId,
        long accessTokenExpiration
) {
}
