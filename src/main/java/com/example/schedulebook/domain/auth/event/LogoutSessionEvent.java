package com.example.schedulebook.domain.auth.event;

public record LogoutSessionEvent(
        Long userId,
        String ip,
        String userAgent
) {
}
