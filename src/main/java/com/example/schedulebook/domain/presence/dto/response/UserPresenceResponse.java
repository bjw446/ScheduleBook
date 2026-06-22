package com.example.schedulebook.domain.presence.dto.response;

public record UserPresenceResponse(
        Long userId,
        boolean online,
        int activeSessions
) {
}
