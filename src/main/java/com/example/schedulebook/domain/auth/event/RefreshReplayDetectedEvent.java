package com.example.schedulebook.domain.auth.event;

public record RefreshReplayDetectedEvent(
        Long userId,
        String loginId,
        String ip,
        String userAgent
) {
}
