package com.example.schedulebook.domain.auth.event;

public record LoginFailedEvent(
        String loginId,
        String ip,
        String userAgent
) {
}
