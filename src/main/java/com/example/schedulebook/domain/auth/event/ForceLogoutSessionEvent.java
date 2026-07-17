package com.example.schedulebook.domain.auth.event;

public record ForceLogoutSessionEvent(
        Long userId,
        String sessionId,
        long accessTokenExpiration
) {
}
