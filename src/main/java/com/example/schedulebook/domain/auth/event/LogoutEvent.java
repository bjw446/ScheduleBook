package com.example.schedulebook.domain.auth.event;

public record LogoutEvent(
        Long userId,
        String ip,
        String userAgent
) {
}
